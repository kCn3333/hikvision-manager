package com.kcn.hikvisionmanager.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RecordingItemDTO {
    private String recordingId;
    private String trackId;

    @NotNull(message = "Recording start time is not defined")
    @PastOrPresent(message = "Recording start time cannot be in the future")
    private LocalDateTime startTime; // Local time

    @PastOrPresent(message = "Recording end time cannot be in the future")
    @NotNull(message = "Recording end time is not defined")
    private LocalDateTime endTime;   // Local time

    private String duration;
    private String codec;

    @NotBlank(message = "Playback URL is missing or invalid")
    private String playbackUrl;

    private String fileSize;          // Size in MB
    private boolean hasMoreResults;

    /**
     * Class-level validation to ensure that endTime is after startTime.
     * This runs automatically when using @Valid in controller.
     */
    @AssertTrue(message = "Recording end time cannot be before start time")
    public boolean isValidTimeRange() {
        if (startTime == null || endTime == null) {
            return true; // handled by @NotNull separately
        }
        return !endTime.isBefore(startTime);
    }

}