package com.kcn.hikvisionmanager.dto;

import com.kcn.hikvisionmanager.domain.DownloadStatus;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DownloadStatusDTO {

    private String jobId;
    private DownloadStatus status;
    private String message;
    private String filePath;

    // Progress info (for DOWNLOADING status)
    private Integer progressPercent;
    private double downloadSpeed;       // Mbps
    private String downloadedSize;     // "112 MB"
    private String totalSize;          // "249 MB"
    private String eta;                // "2m 30s"

    // Download URL (for COMPLETED status)
    private String downloadUrl;
    private String fileName;
    private String actualFileSize;     // Real size after completion

    // Error info (for FAILED status)
    private String errorMessage;
}
