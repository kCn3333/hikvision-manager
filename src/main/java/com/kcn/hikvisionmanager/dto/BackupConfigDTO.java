package com.kcn.hikvisionmanager.dto;

import com.kcn.hikvisionmanager.domain.BackupTimeRangeStrategy;
import com.kcn.hikvisionmanager.domain.ScheduleType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import jakarta.validation.constraints.*;
import lombok.NoArgsConstructor;

/**
 * DTO for camera backup configuration.
 * Holds cron schedule, retention days and backup path.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BackupConfigDTO {

    private String id;

    @NotBlank(message = "Name cannot be blank")
    @Size(min = 3, max = 100, message = "Name must be between 3 and 100 characters long")
    private String name;

    @NotBlank(message = "Camera ID is required")
    private String cameraId; // must match an existing camera (validated in service layer)

    private boolean enabled;

    @NotNull(message = "Schedule type is required")
    private ScheduleType scheduleType; // DAILY, WEEKLY, CUSTOM

    @Pattern(
            regexp = "^(?:[01]?\\d|2[0-3]):[0-5]\\d$",
            message = "Time must be in HH:mm format"
    )
    private String time; // e.g. "02:30"

    @Min(value = 1, message = "Retention days must be at least 1")
    @Max(value = 30, message = "Retention days cannot exceed 365")
    private int retentionDays;

    private boolean notifyOnComplete;

    private String dayOfWeek; // used only for the WEEKLY schedule

    // Nullable - frontend doesn't need to provide it yet
    private BackupTimeRangeStrategy timeRangeStrategy;

}
