package com.kcn.hikvisionmanager.service.backup;

import com.kcn.hikvisionmanager.entity.BackupRecordingEntity;
import com.kcn.hikvisionmanager.domain.BackupRecordingStatus;
import com.kcn.hikvisionmanager.events.model.*;
import com.kcn.hikvisionmanager.repository.BackupJobRepository;
import com.kcn.hikvisionmanager.repository.BackupRecordingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Event handler for backup-related events
 * Updates BackupRecordingEntity status based on download events
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class BackupEventHandler {

    private final BackupRecordingRepository backupRecordingRepository;
    private final BackupJobRepository backupJobRepository;
    private final BackupExecutor backupExecutor;

    // ===== EVENT LISTENERS (RECORDINGS) =====

    /**
     * Update individual BackupRecordingEntity when download starts
     */
    @Async
    @EventListener
    @Transactional
    public void onDownloadStarted(RecordingDownloadStartedEvent event) {
        log.debug("\uD83D\uDC42 Download started event received: {}", event.recordingId());

        BackupRecordingEntity recording = backupRecordingRepository
                .findByBackupJobIdAndRecordingId(event.batchId(), event.recordingId())
                .orElse(null);

        if (recording != null) {
            recording.setStatus(BackupRecordingStatus.DOWNLOADING);
            backupRecordingRepository.save(recording);

            log.debug("💾 BackupRecordingEntity updated: {}", recording.getFileName());
        }
    }

    /**
     * Update individual BackupRecordingEntity when download completes
     */
    @Async
    @EventListener
    @Transactional
    public void onDownloadCompleted(RecordingDownloadCompletedEvent event) {
        log.debug("\uD83D\uDC42 Download completed event received: {}", event.recordingId());

        // 1. Update BackupRecordingEntity
        BackupRecordingEntity recording = backupRecordingRepository
                .findByBackupJobIdAndRecordingId(event.batchId(), event.recordingId())
                .orElse(null);

        if (recording != null) {
            recording.setStatus(BackupRecordingStatus.COMPLETED);
            recording.setFileSizeBytes(event.actualFileSizeBytes());
            recording.setDownloadedAt(event.occurredAt());
            backupRecordingRepository.save(recording);

            // 2. Atomic increment in BackupJob
            backupJobRepository.incrementCompleted(
                    event.batchId(),
                    event.actualFileSizeBytes()
            );

            log.debug("💾 BackupRecordingEntity updated: {}, ", recording.getFileName());
        }
    }

    /**
     * Update individual BackupRecordingEntity when download fails
     */
    @Async
    @EventListener
    @Transactional
    public void onDownloadFailed(RecordingDownloadFailedEvent event) {
        log.info("\uD83D\uDC42 Download failed event received: {}", event.recordingId());

        BackupRecordingEntity recording = backupRecordingRepository
                .findByBackupJobIdAndRecordingId(event.batchId(), event.recordingId())
                .orElse(null);

        if (recording != null) {
            recording.setStatus(BackupRecordingStatus.FAILED);
            recording.setFileSizeBytes(event.actualFileSizeBytes());
            recording.setErrorMessage(event.errorMessage());
            recording.setDownloadedAt(event.occurredAt());
            backupRecordingRepository.save(recording);

            // Atomic increment
            backupJobRepository.incrementFailed(event.batchId());

            log.debug("💾 BackupRecordingEntity updated: {}", recording.getFileName());
        }
    }

    // ===== EVENT LISTENERS (BACKUPS) =====

    /**
     * Finalize backup when entire batch completes
     */
    @EventListener
    public void onBackupBatchCompleted(BackupDownloadCompletedEvent event) {
        String backupId = event.batchId();

        log.debug("\uD83D\uDC42 Backup batch completed event received: {}", backupId);

        backupExecutor.finalizeBackup(backupId);
    }

    /**
     * Finalize backup when entire batch completes
     */
    @EventListener
    public void onBackupBatchFailed(BackupDownloadFailedEvent event) {
        String backupId = event.batchId();

        log.info("❌ Backup batch failed event received: {}", backupId);

        backupExecutor.finalizeBackup(backupId);
    }
}
