package com.kcn.hikvisionmanager.exception;

import com.kcn.hikvisionmanager.domain.BackupJobStatus;
import lombok.Getter;

/**
 * Exception thrown when backup execution fails.
 * Contains backup job context for better error tracking.
 */
@Getter
public class BackupExecutionException extends RuntimeException {

    private final String backupJobId;
    private final BackupJobStatus status;

    public BackupExecutionException(String message, String backupJobId, BackupJobStatus status) {
        super(message);
        this.backupJobId = backupJobId;
        this.status = status;
    }

    public BackupExecutionException(String message, Throwable cause, String backupJobId, BackupJobStatus status) {
        super(message, cause);
        this.backupJobId = backupJobId;
        this.status = status;
    }
}