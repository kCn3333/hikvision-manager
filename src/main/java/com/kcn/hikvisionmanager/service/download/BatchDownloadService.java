package com.kcn.hikvisionmanager.service.download;

import com.kcn.hikvisionmanager.config.DownloadConfig;
import com.kcn.hikvisionmanager.domain.DownloadJob;
import com.kcn.hikvisionmanager.domain.DownloadStatus;
import com.kcn.hikvisionmanager.domain.BatchDownloadJob;
import com.kcn.hikvisionmanager.domain.BatchDownloadStatus;
import com.kcn.hikvisionmanager.dto.BatchStatusDTO;
import com.kcn.hikvisionmanager.dto.RecordingItemDTO;
import com.kcn.hikvisionmanager.events.model.RecordingDownloadCompletedEvent;
import com.kcn.hikvisionmanager.events.model.RecordingDownloadFailedEvent;
import com.kcn.hikvisionmanager.events.publishers.BackupDownloadPublisher;
import com.kcn.hikvisionmanager.exception.JobNotFoundException;
import com.kcn.hikvisionmanager.mapper.BatchDownloadJobMapper;
import com.kcn.hikvisionmanager.repository.BatchDownloadJobRepository;
import com.kcn.hikvisionmanager.repository.DownloadJobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;


import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Service for managing batch downloads
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BatchDownloadService {

    private final RecordingDownloadService downloadService;
    private final BatchDownloadJobRepository batchRepository;
    private final DownloadJobRepository downloadJobRepository;
    private final BatchDownloadJobMapper batchMapper;
    private final BackupDownloadPublisher publisher;
    private final DownloadConfig config;


    /**
     * Start batch download for multiple recordings
     *
     * @param recordings List of recordings to download
     * @return Batch ID for tracking
     */
    public String startBatchDownload(List<RecordingItemDTO> recordings) {
        return startBatchDownload(recordings, null);
    }
    /**
     * Start batch download for multiple recordings with custom target path
     *
     * @param recordings List of recordings to download
     * @param customPath Custom directory path (null = use default /tmp)
     * @return Batch ID for tracking
     */
    public String startBatchDownload(List<RecordingItemDTO> recordings, Path customPath){
        return startBatchDownload(recordings,customPath, null);
    }

    /**
     * Start batch download for multiple recordings with custom target path
     *
     * @param recordings List of recordings to download
     * @param customPath Custom directory path (null = use default /tmp)
     * @param backupJobId If provided, this is a backup batch (isBackup = true)
     * @return Batch ID for tracking
     */
    public String startBatchDownload(List<RecordingItemDTO> recordings, Path customPath, String backupJobId) {
        log.info("🚀 Starting batch download for {} recordings{}",
                recordings.size(),
                customPath != null ? " to " + customPath : "");

        if (recordings.isEmpty()) {
            throw new IllegalArgumentException("Cannot start batch download with empty recording list");
        }

        String batchId, name;
        // check is it backup or manual
        if(backupJobId==null) {
            name="Batch";
            batchId = name+"-"+ UUID.randomUUID();
        }
        else {
            batchId=backupJobId;
            name="Backup";
        }

        // Submit individual downloads
        List<String> jobIds = new ArrayList<>();
        for (RecordingItemDTO recording : recordings) {
            String jobId = downloadService.startDownload(recording, customPath, batchId, name);
            jobIds.add(jobId);
        }

        // Create batch job
        BatchDownloadJob batch = BatchDownloadJob.builder()
                .batchId(batchId)
                .jobIds(jobIds)
                .batchDownloadPath(customPath!=null ? customPath : config.getDownloadPath())
                .totalRecordings(recordings.size())
                .completedRecordings(0)
                .failedRecordings(0)
                .status(BatchDownloadStatus.IN_PROGRESS)
                .createdAt(LocalDateTime.now())
                .build();

        batchRepository.save(batch);
        if(backupJobId!=null) {
            publisher.publishBackupStarted(batch.getBatchId());
        }

        log.debug("✅ [{}] {} download created: {} ({} recordings with jobId {})", Thread.currentThread().getName(), name, batchId, recordings.size(), jobIds);

        return batchId;
    }

    /**
     * Listen for download completions and check if batch is done
     */
    @EventListener
    public void onDownloadCompleted(RecordingDownloadCompletedEvent event) {
        log.debug("\uD83D\uDC42 [{}] Download completed event received: {}", Thread.currentThread().getName(), event.recordingId());

        checkBatchCompletion(event.batchId());
    }

    @EventListener
    public void onDownloadFailed(RecordingDownloadFailedEvent event) {
        log.warn("\uD83D\uDC42 [{}] Download failed event received: {}", Thread.currentThread().getName(), event.recordingId());

        checkBatchCompletion(event.batchId());
    }

    /**
     * Check batch status and publish completion event if finished
     * Called by event listener when individual download completes
     */
    public void checkBatchCompletion(String batchId) {
        BatchDownloadJob batch = batchRepository.findById(batchId).orElse(null);

        if (batch == null) {
            log.warn("⚠️  Batch not found: {}", batchId);
            return;
        }

        if (batch.getStatus() == BatchDownloadStatus.COMPLETED ||
                batch.getStatus() == BatchDownloadStatus.FAILED) {
            // Already finalized
            return;
        }

        // Get all job statuses
        List<DownloadJob> jobs = batch.getJobIds().stream()
                .map(jobId -> downloadJobRepository.findById(jobId).orElse(null))
                .filter(job -> job != null)
                .toList();

        // Count statuses
        long completed = jobs.stream()
                .filter(j -> j.getStatus() == DownloadStatus.COMPLETED)
                .count();

        long failed = jobs.stream()
                .filter(j -> j.getStatus() == DownloadStatus.FAILED ||
                        j.getStatus() == DownloadStatus.CANCELLED)
                .count();

        long pending = jobs.stream()
                .filter(j -> j.getStatus() == DownloadStatus.QUEUED ||
                        j.getStatus() == DownloadStatus.DOWNLOADING)
                .count();

        // Update batch progress
        batch.setCompletedRecordings((int) completed);
        batch.setFailedRecordings((int) failed);

        log.debug("📊 Batch {} progress: {}/{} completed, {} failed, {} pending",
                batchId, completed, batch.getTotalRecordings(), failed, pending);

        // Check if all jobs finished
        if (pending == 0) {
            finalizeBatch(batch, jobs);
        } else {
            // Save progress
            batchRepository.save(batch);
        }
    }

    /**
     * Finalize batch and publish completion event
     */
    private void finalizeBatch(BatchDownloadJob batch, List<DownloadJob> jobs) {
        long completed = batch.getCompletedRecordings();
        long failed = batch.getFailedRecordings();

        // Determine final status
        if (completed == batch.getTotalRecordings()) {
            batch.setStatus(BatchDownloadStatus.COMPLETED);
            publisher.publishBackupCompleted(batch.getBatchId());
        } else if (completed == 0) {
            batch.setStatus(BatchDownloadStatus.FAILED);
            publisher.publishBackupFailed(batch.getBatchId());
        } else {
            batch.setStatus(BatchDownloadStatus.PARTIAL_FAILURE);
            publisher.publishBackupFailed(batch.getBatchId());
        }

        batchRepository.save(batch);

        log.debug("🎉 Batch finalized: {} - {} ({}/{} completed)",
                batch.getBatchId(), batch.getStatus(), completed, batch.getTotalRecordings());


    }

    /**
     * Get batch download status
     */
    public BatchStatusDTO getBatchStatus(String batchId) {
        BatchDownloadJob batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new IllegalArgumentException("Batch not found: " + batchId));

        // Get individual job statuses
        List<DownloadJob> jobs = batch.getJobIds().stream()
                .map(jobId -> downloadJobRepository.findById(jobId)
                        .orElseThrow(() -> new JobNotFoundException(jobId)))
                .toList();

        // Map to DTO
        return batchMapper.toStatusDTO(batch, jobs);
    }

    /**
     * Check if batch is completed (all jobs finished)
     */
    public boolean isBatchCompleted(String batchId) {
        BatchStatusDTO status = getBatchStatus(batchId);
        return status.getStatus() == BatchDownloadStatus.COMPLETED ||
                status.getStatus() == BatchDownloadStatus.FAILED ||
                status.getStatus() == BatchDownloadStatus.PARTIAL_FAILURE;
    }

    /**
     * Cancel entire batch (cancel all pending jobs)
     */
    public void cancelBatch(String batchId) {
        BatchDownloadJob batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new IllegalArgumentException("Batch not found: " + batchId));

        log.info("🚫 Cancelling batch: {}", batchId);

        // Cancel all individual jobs
        for (String jobId : batch.getJobIds()) {
            try {
                downloadService.cancelDownload(jobId);
            } catch (Exception e) {
                log.warn("Failed to cancel job {}: {}", jobId, e.getMessage());
            }
        }

        batch.setStatus(BatchDownloadStatus.FAILED);
        batch.setErrorMessage("Cancelled by user");
        batchRepository.save(batch);
        publisher.publishBackupFailed(batch.getBatchId());
    }
}