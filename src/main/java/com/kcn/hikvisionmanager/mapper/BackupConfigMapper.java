package com.kcn.hikvisionmanager.mapper;

import com.kcn.hikvisionmanager.config.BackupConfig;
import com.kcn.hikvisionmanager.entity.BackupConfigurationEntity;
import com.kcn.hikvisionmanager.domain.BackupTimeRangeStrategy;
import com.kcn.hikvisionmanager.domain.ScheduleType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import com.kcn.hikvisionmanager.dto.BackupConfigDTO;

import java.time.LocalDateTime;

/**
 * Mapper for BackupConfigurationEntity ↔ BackupConfigDTO conversion.
 * Simple field-to-field mapping, no date/time conversion needed.
 */
@Component
@RequiredArgsConstructor
public class BackupConfigMapper {

    private final BackupConfig backupConfig;

    /**
     * Maps BackupConfigDTO to BackupConfigurationEntity entity.
     * Generates ID, CRON expression, and timestamps.
     */
    public BackupConfigurationEntity toEntity(BackupConfigDTO dto) {
        if (dto == null) {
            return null;
        }

        return BackupConfigurationEntity.builder()
                .id(generateIdIfNull(dto.getId()))
                .name(dto.getName())
                .cameraId(dto.getCameraId())
                .enabled(dto.isEnabled())
                .backupPath(backupConfig.getBaseDir().toString())
                .cronExpression(generateCron(dto))
                .retentionDays(dto.getRetentionDays())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .timeRangeStrategy(dto.getTimeRangeStrategy() != null
                        ? dto.getTimeRangeStrategy()
                        : BackupTimeRangeStrategy.LAST_24_HOURS)
                .build();
    }

    /**
     * Maps BackupConfigurationEntity entity to BackupConfigDTO for API responses.
     */
    public BackupConfigDTO toDTO(BackupConfigurationEntity entity) {
        if (entity == null) {
            return null;
        }

        return BackupConfigDTO.builder()
                .id(entity.getId())
                .name(entity.getName())
                .cameraId(entity.getCameraId())
                .enabled(entity.isEnabled())
                .scheduleType(parseCron(entity.getCronExpression()).scheduleType)
                .time(parseCron(entity.getCronExpression()).time)
                .dayOfWeek(parseCron(entity.getCronExpression()).dayOfWeek)
                .retentionDays(entity.getRetentionDays())
                .notifyOnComplete(false)
                .timeRangeStrategy(entity.getTimeRangeStrategy() != null
                        ? entity.getTimeRangeStrategy()
                        : BackupTimeRangeStrategy.LAST_24_HOURS)
                .build();
    }


    private String generateIdIfNull(String id) {
        return (id == null || id.isBlank()) ? "backup-" + System.currentTimeMillis() : id;
    }

    /**
     * Builds CRON expression from user-friendly scheduling fields.
     */
    /**
     * Builds CRON expression from user-friendly scheduling fields.
     */
    public String generateCron(BackupConfigDTO dto) {
        // Validation for standard types
        if (dto.getScheduleType() != ScheduleType.CUSTOM && (dto.getTime() == null || dto.getTime().isBlank())) {
            throw new IllegalArgumentException("Time must be provided for schedule generation");
        }

        // Handle CUSTOM type
        if (dto.getScheduleType() == ScheduleType.CUSTOM) {
            // For future 'cronExpression' field in DTO
            // For now, return a safe default or specific logic instead of crashing.
            // return dto.getCronExpression() != null ? dto.getCronExpression() : "0 0 0 * * *";
            return "0 0 0 * * *"; // Placeholder to prevent crash
        }

        String[] timeParts = dto.getTime().split(":");
        int hour = Integer.parseInt(timeParts[0]);
        int minute = Integer.parseInt(timeParts[1]);

        return switch (dto.getScheduleType()) {
            case DAILY -> String.format("0 %d %d * * *", minute, hour);
            case WEEKLY -> {
                // Fix: Handle null dayOfWeek safely
                String dayInput = dto.getDayOfWeek();
                String day = (dayInput != null && dayInput.length() >= 3)
                        ? dayInput.substring(0, 3).toUpperCase()
                        : "MON"; // Default to Monday if missing
                yield String.format("0 %d %d ? * %s", minute, hour, day);
            }
            default -> throw new IllegalArgumentException("Unknown schedule type");
        };
    }

    /**
     * Parses CRON expression into readable schedule fields.
     * Simplified logic (supports typical DAILY and WEEKLY patterns).
     */
    private CronInfo parseCron(String cron) {
        CronInfo info = new CronInfo();

        if (cron == null || cron.isEmpty()) {
            info.scheduleType = ScheduleType.DAILY;
            info.time = "00:00";
            return info;
        }

        try {
            String[] parts = cron.split(" ");
            if (parts.length >= 6 && parts[5].equals("*")) {
                info.scheduleType = ScheduleType.DAILY;
                info.time = String.format("%02d:%02d", Integer.parseInt(parts[2]), Integer.parseInt(parts[1]));
            } else if (parts.length >= 6) {
                info.scheduleType = ScheduleType.WEEKLY;
                info.time = String.format("%02d:%02d", Integer.parseInt(parts[2]), Integer.parseInt(parts[1]));
                info.dayOfWeek = parts[5];
            } else {
                info.scheduleType = ScheduleType.CUSTOM;
                info.time = "00:00";
            }
        } catch (Exception e) {
            info.scheduleType = ScheduleType.CUSTOM;
            info.time = "00:00";
        }

        return info;
    }

    private static class CronInfo {
        private ScheduleType scheduleType;
        private String time;
        private String dayOfWeek;
    }
}