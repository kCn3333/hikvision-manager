package com.kcn.hikvisionmanager.events.publishers;

import com.kcn.hikvisionmanager.domain.BatchDownloadJob;
import com.kcn.hikvisionmanager.events.model.BackupDownloadCompletedEvent;
import com.kcn.hikvisionmanager.events.model.BackupDownloadFailedEvent;
import com.kcn.hikvisionmanager.events.model.BackupDownloadStartedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@Slf4j
@RequiredArgsConstructor
public class BackupDownloadPublisher {

    private final EventPublisherHelper eventPublisherHelper;

    public void publishBackupStarted(String backupBatchId) {
        log.debug("Thread: [{}] tryin to publish BackupStartedEvent",Thread.currentThread().getName());
        try {
            eventPublisherHelper.publish(new BackupDownloadStartedEvent(
                    backupBatchId,
                    LocalDateTime.now()
            ));
            log.debug("\uD83D\uDCE3 Published BackupStartedEvent for batch: {}", backupBatchId);

        } catch (Exception e) {
            log.warn("⛔ Failed to publish BackupStartedEvent: {}", e.getMessage());
        }

    }

    public void publishBackupCompleted(String backupBatchId) {
        log.debug("Thread: [{}] tryin to publish BackupCompletedEvent",Thread.currentThread().getName());
        try {
            eventPublisherHelper.publish(new BackupDownloadCompletedEvent(
                    backupBatchId,
                    LocalDateTime.now()
            ));
            log.debug("\uD83D\uDCE3 Published BackupCompletedEvent for batch: {}", backupBatchId);

        } catch (Exception e) {
            log.warn("⛔ Failed to publish BackupCompletedEvent: {}", e.getMessage());
        }
    }

    public void publishBackupFailed(String backupBatchId) {
        log.debug("Thread: [{}] tryin to publish BackupFailedEvent",Thread.currentThread().getName());
        try {
            eventPublisherHelper.publish(new BackupDownloadFailedEvent(
                    backupBatchId,
                    LocalDateTime.now()
            ));
            log.warn("\uD83D\uDCE3 Published BackupFailedEvent for batch: {}", backupBatchId);

        } catch (Exception e) {
            log.warn("⛔ Failed to publish BackupBatchCompletedEvent: {}", e.getMessage());
        }

    }
}
