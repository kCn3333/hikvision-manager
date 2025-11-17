package com.kcn.hikvisionmanager.domain;


import java.time.LocalDateTime;

/**
 * Represents a date/time range for backup operations.
 *
 * @param start Start date/time (inclusive)
 * @param end End date/time (inclusive)
 */
public record BackupDateRange(LocalDateTime start, LocalDateTime end) {

    /**
     * Validates that start is before end
     */
    public BackupDateRange {
        if (start.isAfter(end)) {
            throw new IllegalArgumentException(
                    String.format("Start time (%s) must be before end time (%s)", start, end)
            );
        }
    }
}