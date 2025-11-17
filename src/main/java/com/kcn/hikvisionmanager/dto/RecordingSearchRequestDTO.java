package com.kcn.hikvisionmanager.dto;

import jakarta.validation.constraints.PastOrPresent;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.AssertTrue;


import java.time.LocalDateTime;


/**
 * DTO representing search criteria for camera recordings.
 * Automatically validated by Bean Validation during @Valid binding in the controller.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecordingSearchRequestDTO {

    @NotNull(message = "Start time cannot be null")
    @PastOrPresent(message = "Start time cannot be in the future")
    private LocalDateTime startTime;

    @NotNull(message = "End time cannot be null")
    @PastOrPresent(message = "End time cannot be in the future")
    private LocalDateTime endTime;

    @Min(value = 0, message = "Page cannot be negative")
    private int page=0;

    @Min(value = 1, message = "Page size must be greater than zero")
    private int pageSize=10;

    /**
     * Validates that endTime is after startTime.
     * Triggered automatically by @Valid in controller.
     */
    @AssertTrue(message = "End time must be after start time")
    public boolean isTimeRangeValid() {
        if (startTime == null || endTime == null) {
            return true; // handled by @NotNull separately
        }
        return endTime.isAfter(startTime);
    }
}