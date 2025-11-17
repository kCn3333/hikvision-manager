package com.kcn.hikvisionmanager.dto;

import com.kcn.hikvisionmanager.domain.BatchDownloadStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO for batch download status
 */
@Data
@Builder
public class BatchStatusDTO {
    private String batchId;
    private BatchDownloadStatus status;
    private int total;
    private int completed;
    private int inProgress;
    private int failed;
    private int queued;
    private String message;
    private String path;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
    private List<DownloadStatusDTO> jobs;
}
