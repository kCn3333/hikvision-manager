package com.kcn.hikvisionmanager.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;


/**
 * DTO representing a single backup execution/job.
 * Times are in user's local timezone for display purposes.
 */
@Data
@Builder
public class BackupJobDTO {

    private String jobId;              // Unique job ID
    private String cameraId;           // Camera reference
    private LocalDateTime startedAt;   // Local start time
    private LocalDateTime endTime;     // Local end time
    private long totalFiles;           // Number of files backed up
    private long completedFiles;       // Number of files completed
    private long totalBytes;           // Total size of files backed up
    private String status;             // Status: RUNNING, SUCCESS, FAILED
    private String logPath;            // Path to a backup log file
}
