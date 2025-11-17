package com.kcn.hikvisionmanager.domain;

import lombok.Builder;
import lombok.Data;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class BatchDownloadJob {
    private String batchId;
    private List<String> jobIds;
    private int totalRecordings;
    private int completedRecordings;
    private int failedRecordings;
    private BatchDownloadStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
    private Path batchDownloadPath;
    private String errorMessage;
}

