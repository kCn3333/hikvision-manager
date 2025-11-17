package com.kcn.hikvisionmanager.domain;

import lombok.Builder;
import lombok.Data;

import java.nio.file.Path;
import java.time.LocalDateTime;

@Data
@Builder
public class DownloadJob {
    private String jobId;
    private String batchId;
    private boolean isBackupJob=false;
    private String recordingId;
    private String trackId;

    // Timing info
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String duration;           // Format: "10:26"

    // Progress tracking
    private DownloadStatus status;
    private int progressPercent;
    private double downloadSpeed;         // Mbps
    private String currentTime;        // Format: "00:04:42"
    private long downloadedBytes;      // Estimated downloaded bytes
    private long totalBytes;           // Expected total bytes from fileSize
    private String eta;                // Format: "2m 30s"

    // File info
    private Path filePath;
    private String fileName;
    private Long actualFileSizeBytes;  // Real size after download completes

    // Timestamps
    private LocalDateTime createdAt;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;

    // Error handling
    private String errorMessage;

    // RTSP URL with credentials
    private String rtspUrl;
}
