package com.kcn.hikvisionmanager.service.notification;

import com.kcn.hikvisionmanager.domain.BackupJobStatus;
import com.kcn.hikvisionmanager.entity.BackupConfigurationEntity;
import com.kcn.hikvisionmanager.entity.BackupJobEntity;
import com.kcn.hikvisionmanager.events.model.BackupDownloadCompletedEvent;
import com.kcn.hikvisionmanager.events.model.BackupDownloadFailedEvent;
import com.kcn.hikvisionmanager.repository.BackupConfigurationRepository;
import com.kcn.hikvisionmanager.repository.BackupJobRepository;
import com.kcn.hikvisionmanager.service.backup.BackupStatisticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Event listener for backup completion/failure events.
 * Sends notifications via configured notification services when backups finish.
 *
 * Only sends notifications if:
 * 1. Backup configuration has notifyOnComplete=true
 * 2. At least one notification service is configured
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationListener {

    private final BackupJobRepository backupJobRepository;
    private final BackupConfigurationRepository backupConfigurationRepository;
    private final BackupStatisticsService backupStatisticsService;
    private final NotificationMessageBuilder messageBuilder;
    private final List<NotificationService> notificationServices;

    /**
     * Handles backup completion event.
     * Sends success notification if enabled.
     */
    @Async
    @EventListener
    public void onBackupCompleted(BackupDownloadCompletedEvent event) {
        log.debug("👂 Backup completed event received: {}", event.batchId());
        handleBackupEvent(event.batchId(), BackupJobStatus.COMPLETED);
    }

    /**
     * Handles backup failure event.
     * Sends failure notification if enabled.
     */
    @Async
    @EventListener
    public void onBackupFailed(BackupDownloadFailedEvent event) {
        log.debug("👂 Backup failed event received: {}", event.batchId());
        handleBackupEvent(event.batchId(), BackupJobStatus.FAILED);
    }

    /**
     * Core notification handling logic.
     * Checks if notifications are enabled, fetches stats, builds message, and sends.
     */
    private void handleBackupEvent(String batchId, BackupJobStatus status) {
        try {
            // Check if any notification services are configured
            if (notificationServices.isEmpty()) {
                log.debug("No notification services configured - skipping notification");
                return;
            }

            // Get backup job
            BackupJobEntity job = backupJobRepository.findById(batchId).orElse(null);
            if (job == null) {
                log.warn("⚠️ Backup job not found for notification: {}", batchId);
                return;
            }

            // Get backup configuration
            BackupConfigurationEntity config = backupConfigurationRepository
                    .findById(job.getConfigId()).orElse(null);

            if (config == null) {
                log.warn("⚠️ Backup configuration not found for job: {}", batchId);
                return;
            }

            // Check if notifications are enabled for this backup
            if (!config.isNotifyOnComplete()) {
                log.debug("Notifications disabled for backup '{}' - skipping", config.getName());
                return;
            }

            // Get job statistics
            Map<String, Object> stats = backupStatisticsService.getJobStatistics(batchId);

            // Build notification content
            String title = buildNotificationTitle(config.getName(), status);
            String message = messageBuilder.buildBackupSummary(stats, status);

            log.info("🔔 Sending backup notification: {}", title);

            // Send via all configured notification services
            for (NotificationService service : notificationServices) {
                try {
                    service.send(title, message);
                    log.debug("✅ Notification sent via {}", service.getType());
                } catch (Exception e) {
                    log.error("❌ Failed to send notification via {}: {}",
                            service.getType(), e.getMessage());
                    // Continue with other services - don't fail entire notification flow
                }
            }

        } catch (Exception e) {
            log.error("❌ Error in notification handler for backup {}: {}",
                    batchId, e.getMessage(), e);
            // Don't throw - notification failures shouldn't affect backup process
        }
    }

    /**
     * Builds notification title from backup name and status.
     */
    private String buildNotificationTitle(String backupName, BackupJobStatus status) {
        String statusEmoji = status == BackupJobStatus.COMPLETED ? "✅" : "❌";
        return String.format("%s Backup: %s", statusEmoji, backupName);
    }

}