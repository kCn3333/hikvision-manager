package com.kcn.hikvisionmanager.service.download;

import com.kcn.hikvisionmanager.config.DownloadConfig;
import com.kcn.hikvisionmanager.domain.DownloadJob;
import com.kcn.hikvisionmanager.domain.DownloadStatus;
import com.kcn.hikvisionmanager.dto.RecordingItemDTO;
import com.kcn.hikvisionmanager.events.publishers.RecordingDownloadPublisher;
import com.kcn.hikvisionmanager.repository.DownloadJobRepository;
import com.kcn.hikvisionmanager.service.CameraUrlBuilder;
import com.kcn.hikvisionmanager.util.ProgressCalculator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.UUID;

import static com.kcn.hikvisionmanager.util.FileNameUtils.generateFileName;

/**
 * Service for managing recording downloads
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RecordingDownloadService {

    private final DownloadJobRepository repository;
    private final DownloadJobQueue jobQueue;
    private final DownloadConfig config;
    private final CameraDownloadSemaphore cameraSemaphore;
    private final CameraUrlBuilder urlBuilder;
    private final RecordingDownloadPublisher publisher;


    /**
     * Start download for a recording.
     * This always associates the job with a batch — either a real one or the special "single" batch.
     */
    public String startDownload(RecordingItemDTO recording) {
        return startDownload(recording, null, null, null);
    }

    /**
     * Start download for a recording with a custom target path.
     */
    public String startDownload(RecordingItemDTO recording, Path customPath) {
        return startDownload(recording, customPath, null, null);
    }

    /**
     * Start download for a recording with full control over batch info and path.
     */
    public String startDownload(RecordingItemDTO recording, Path customPath, String batchId, String batchName) {

        log.debug("[DownloadService] got: {}, customPath={}, batchId={}, batchName={}", recording.getRecordingId(), customPath, batchId, batchName);


        if (batchName == null || batchName.isBlank()) {
            batchName = "single";
        }
        if (batchId == null || batchId.isBlank()) {
            batchId = batchName + "-" + UUID.randomUUID();
        }

        log.debug("🚀 Starting download request for recording: {}, {} (batch: {})",
                recording.getRecordingId(), batchName, batchId);


        // Determine target path
        Path targetPath = (customPath != null) ? customPath : config.getDownloadPath();

        // Clean up old downloads if configured (only for default path)
        if (customPath == null && config.isKeepLastCompletedOnly()) {
            cleanupPreviousDownloads();
        }

        // Create and save job
        DownloadJob job = createDownloadJob(recording, targetPath, batchId, batchName);
        repository.save(job);

        // Queue job for execution
        jobQueue.submit(job);

        log.debug("✅ Download job queued: {} (Job ID: {}, Batch: {})",
                job.getFileName(), job.getJobId(), batchName);

        return job.getJobId();
    }

    /**
     * Get current download status for a job.
     */
    public DownloadJob getDownloadStatus(String jobId) {
        return repository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Download job not found: " + jobId));
    }

    /**
     * Get download file path
     */
    public Path getDownloadFile(String jobId) {
        DownloadJob job = getDownloadStatus(jobId);

        if (job.getStatus() != DownloadStatus.COMPLETED) {
            throw new IllegalStateException("Download not completed yet: " + jobId);
        }

        Path filePath = job.getFilePath();

        if (!Files.exists(filePath)) {
            throw new IllegalStateException("Download file not found: " + filePath);
        }

        return filePath;
    }

    /**
     * Cancel download
     */
    public void cancelDownload(String jobId) {
        DownloadJob job = getDownloadStatus(jobId);

        if (job.getStatus() == DownloadStatus.COMPLETED) {
            throw new IllegalStateException("Cannot cancel completed download");
        }

        if (job.getStatus() == DownloadStatus.QUEUED ||
                job.getStatus() == DownloadStatus.DOWNLOADING) {

            job.setStatus(DownloadStatus.CANCELLED);
            job.setErrorMessage("Cancelled by user");
            repository.save(job);

            log.info("🚫 Download cancelled: {}", jobId);
        }
    }

    /**
     * Create download job from recording DTO
     */
    private DownloadJob createDownloadJob(RecordingItemDTO recording, Path targetPath, String batchId, String batchName) {
        String jobId = UUID.randomUUID().toString();

        // Generate file name: recording_2025-10-30_15-55-28_2025-10-30_16-05-54.mp4
        String fileName = generateFileName(recording);

        Path filePath = targetPath.resolve(fileName);

        // Add credentials to RTSP URL (only for fmpeg)
        String rtspUrl;
        if(!config.isHttpDownload())
            rtspUrl = urlBuilder.addCredentialsToRtspUrl(recording.getPlaybackUrl());
        else rtspUrl=recording.getPlaybackUrl();

        // Parse expected file size to bytes
        long totalBytes = ProgressCalculator.parseFileSize(recording.getFileSize());

        return DownloadJob.builder()
                .jobId(jobId)
                .batchId(batchId)
                .isBackupJob(batchName.equals("Backup"))
                .recordingId(recording.getRecordingId())
                .trackId(recording.getTrackId())
                .startTime(recording.getStartTime())
                .endTime(recording.getEndTime())
                .duration(recording.getDuration())
                .status(DownloadStatus.QUEUED)
                .progressPercent(0)
                .downloadedBytes(0)
                .totalBytes(totalBytes)
                .filePath(filePath)
                .fileName(fileName)
                .createdAt(LocalDateTime.now())
                .rtspUrl(rtspUrl)
                .build();
    }

    /**
     * Clean up previous completed downloads
     */
    private void cleanupPreviousDownloads() {
        repository.findByStatus(DownloadStatus.COMPLETED)
                .forEach(job -> {
                    try {
                        if (job.getFilePath() != null && Files.exists(job.getFilePath())) {
                            Files.delete(job.getFilePath());
                            log.info("🗑️ Deleted previous download: {}", job.getFileName());
                        }
                        repository.delete(job.getJobId());
                    } catch (Exception e) {
                        log.warn("Failed to delete previous download: {}", job.getFileName(), e);
                    }
                });
    }

    /**
     * Cleanup old downloads (called by scheduler)
     */
    public void cleanupOldDownloads() {
        LocalDateTime cutoffTime = LocalDateTime.now()
                .minusHours(config.getMaxRetentionHours());

        repository.findOlderThan(cutoffTime)
                .forEach(job -> {
                    try {
                        if (job.getFilePath() != null && Files.exists(job.getFilePath())) {
                            Files.delete(job.getFilePath());
                            log.info("🗑️ Cleaned up old download: {} (age: {}h)",
                                    job.getFileName(),
                                    java.time.Duration.between(job.getCreatedAt(), LocalDateTime.now()).toHours());
                        }
                        repository.delete(job.getJobId());
                    } catch (Exception e) {
                        log.warn("Failed to cleanup old download: {}", job.getFileName(), e);
                    }
                });
    }

    /**
     * Check if camera is currently busy downloading
     */
    public boolean isCameraBusy() {
        return cameraSemaphore.isBusy();
    }
}

