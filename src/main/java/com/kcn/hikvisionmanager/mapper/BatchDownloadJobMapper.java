package com.kcn.hikvisionmanager.mapper;

import com.kcn.hikvisionmanager.domain.DownloadJob;
import com.kcn.hikvisionmanager.domain.DownloadStatus;
import com.kcn.hikvisionmanager.domain.BatchDownloadJob;
import com.kcn.hikvisionmanager.domain.BatchDownloadStatus;
import com.kcn.hikvisionmanager.dto.BatchStatusDTO;
import com.kcn.hikvisionmanager.dto.DownloadStatusDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Mapper for batch download jobs
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BatchDownloadJobMapper {

    private final DownloadJobMapper downloadJobMapper;

    /**
     * Convert BatchDownloadJob to BatchStatusDTO with individual job details
     */
    public BatchStatusDTO toStatusDTO(BatchDownloadJob batch, List<DownloadJob> individualJobs) {

        // Map individual jobs to DTOs
        List<DownloadStatusDTO> jobDTOs = individualJobs.stream()
                .map(downloadJobMapper::toStatusDTO)
                .toList();

        // Count jobs by status
        int completed = countByStatus(jobDTOs, DownloadStatus.COMPLETED);
        int inProgress = countByStatus(jobDTOs, DownloadStatus.DOWNLOADING);
        int failed = countByStatus(jobDTOs, DownloadStatus.FAILED);
        int queued = countByStatus(jobDTOs, DownloadStatus.QUEUED);

        // Determine batch status
        BatchDownloadStatus status = determineBatchStatus(batch, completed, failed, queued);

        return BatchStatusDTO.builder()
                .batchId(batch.getBatchId())
                .status(status)
                .total(batch.getTotalRecordings())
                .completed(completed)
                .inProgress(inProgress)
                .failed(failed)
                .queued(queued)
                .message(generateStatusMessage(status, completed, batch.getTotalRecordings(), failed))
                .path(batch.getBatchDownloadPath().toString())
                .createdAt(batch.getCreatedAt())
                .completedAt(batch.getCompletedAt())
                .jobs(jobDTOs)
                .build();
    }

    /**
     * Count jobs by status
     */
    private int countByStatus(List<DownloadStatusDTO> jobs, DownloadStatus status) {
        return (int) jobs.stream()
                .filter(job -> job.getStatus() == status)
                .count();
    }

    /**
     * Determine overall batch status based on individual job statuses
     */
    private BatchDownloadStatus determineBatchStatus(BatchDownloadJob batch, int completed, int failed, int queued) {
        int total = batch.getTotalRecordings();

        // All completed
        if (completed == total) {
            return BatchDownloadStatus.COMPLETED;
        }

        // All failed
        if (failed == total) {
            return BatchDownloadStatus.FAILED;
        }

        // Some completed, some failed
        if (completed > 0 && failed > 0 && (completed + failed == total)) {
            return BatchDownloadStatus.PARTIAL_FAILURE;
        }

        // Still in progress
        if (queued > 0 || (completed + failed < total)) {
            return BatchDownloadStatus.IN_PROGRESS;
        }

        return BatchDownloadStatus.IN_PROGRESS;
    }

    /**
     * Generate a human-readable status message
     */
    private String generateStatusMessage(BatchDownloadStatus status, int completed, int total, int failed) {
        return switch (status) {
            case QUEUED -> "Batch download queued";
            case IN_PROGRESS -> String.format("Downloading recordings (%d/%d completed)", completed, total);
            case COMPLETED -> String.format("All %d recordings downloaded successfully", total);
            case PARTIAL_FAILURE -> String.format("%d of %d recordings downloaded (%d failed)", completed, total, failed);
            case FAILED -> "All recordings failed to download";
        };
    }
}