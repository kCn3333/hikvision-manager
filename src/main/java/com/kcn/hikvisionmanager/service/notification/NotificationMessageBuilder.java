package com.kcn.hikvisionmanager.service.notification;

import com.kcn.hikvisionmanager.domain.BackupJobStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Builds formatted notification messages from backup statistics.
 * Creates user-friendly, emoji-enhanced messages for backup summaries.
 */
@Component
@Slf4j
public class NotificationMessageBuilder {

    /**
     * Builds backup summary message from job statistics.
     * Format includes recordings count, failures, size, and duration.
     *
     * @param stats Job statistics from BackupStatisticsService
     * @param status Final backup job status
     * @return Formatted notification message with emojis
     */
    public String buildBackupSummary(Map<String, Object> stats, BackupJobStatus status) {
        StringBuilder message = new StringBuilder();

        // Status header
        if (status == BackupJobStatus.COMPLETED) {
            message.append("✅ **Backup Completed Successfully**\n\n");
        } else if (status == BackupJobStatus.FAILED) {
            message.append("❌ **Backup Failed**\n\n");
        }

        // Recordings summary
        int totalRecordings = getIntValue(stats, "totalRecordings");
        int completedRecordings = getIntValue(stats, "completedRecordings");
        int failedRecordings = getIntValue(stats, "failedRecordings");

        message.append(String.format("📊 **Recordings:** %d/%d completed",
                completedRecordings, totalRecordings));

        if (failedRecordings > 0) {
            message.append(String.format(" (%d failed)", failedRecordings));
        }
        message.append("\n");

        // File size
        String totalSize = getStringValue(stats, "totalSizeFormatted");
        if (totalSize != null && !totalSize.equals("0 B")) {
            message.append(String.format("💾 **Size:** %s\n", totalSize));
        }

        // Duration
        String duration = getStringValue(stats, "durationFormatted");
        if (duration != null) {
            message.append(String.format("⏱️ **Duration:** %s\n", duration));
        }

        // Backup directory
        String backupDir = getStringValue(stats, "backupDirectory");
        if (backupDir != null) {
            message.append(String.format("\n📂 **Location:** `%s`", backupDir));
        }

        // Error message for failed backups
        if (status == BackupJobStatus.FAILED) {
            String errorMessage = getStringValue(stats, "errorMessage");
            if (errorMessage != null) {
                message.append(String.format("\n\n⚠️ **Error:** %s", errorMessage));
            }
        }

        return message.toString();
    }

    /**
     * Safely extracts integer value from stats map.
     */
    private int getIntValue(Map<String, Object> stats, String key) {
        Object value = stats.get(key);
        if (value instanceof Integer) {
            return (Integer) value;
        }
        return 0;
    }

    /**
     * Safely extracts string value from stats map.
     */
    private String getStringValue(Map<String, Object> stats, String key) {
        Object value = stats.get(key);
        return value != null ? value.toString() : null;
    }
}