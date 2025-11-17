package com.kcn.hikvisionmanager.mapper;

import com.kcn.hikvisionmanager.domain.DownloadJob;
import com.kcn.hikvisionmanager.domain.DownloadStatus;
import com.kcn.hikvisionmanager.dto.DownloadStatusDTO;
import com.kcn.hikvisionmanager.util.ProgressCalculator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Mapper for converting DownloadJob to DTOs
 */
@Component
@Slf4j
public class DownloadJobMapper {

    /**
     * Convert DownloadJob to DownloadStatusDTO
     */
    public DownloadStatusDTO toStatusDTO(DownloadJob job) {
        DownloadStatusDTO.DownloadStatusDTOBuilder builder = DownloadStatusDTO.builder()
                .jobId(job.getJobId())
                .status(job.getStatus());

        // Set message based on status
        builder.message(getStatusMessage(job));

        // Add progress info for DOWNLOADING status
        if (job.getStatus() == DownloadStatus.DOWNLOADING) {
            builder.progressPercent(job.getProgressPercent())
                    .downloadSpeed(job.getDownloadSpeed())
                    .filePath(job.getFilePath().toString())
                    .fileName(job.getFileName())
                    .downloadedSize(ProgressCalculator.formatBytes(job.getDownloadedBytes()))
                    .totalSize(ProgressCalculator.formatBytes(job.getTotalBytes()))
                    .eta(job.getEta());
        }

        // Add download info for COMPLETED status
        if (job.getStatus() == DownloadStatus.COMPLETED) {
            builder.downloadUrl("/api/recordings/download/" + job.getJobId() + "/file")
                    .fileName(job.getFileName())
                    .actualFileSize(ProgressCalculator.formatBytes(job.getActualFileSizeBytes()));
        }

        // Add error info for FAILED status
        if (job.getStatus() == DownloadStatus.FAILED) {
            builder.errorMessage(job.getErrorMessage());
        }

        return builder.build();
    }

    /**
     * Get a human-readable status message
     */
    private String getStatusMessage(DownloadJob job) {
        return switch (job.getStatus()) {
            case QUEUED -> "Added to download queue";
            case DOWNLOADING -> {
                if (job.getProgressPercent() == 0) {
                    yield "Camera is busy, waiting...";
                } else {
                    yield "Downloading...";
                }
            }
            case COMPLETED -> "Download completed successfully";
            case FAILED -> "Download failed";
            case CANCELLED -> "Download cancelled";
        };
    }
}
