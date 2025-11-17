package com.kcn.hikvisionmanager.domain;

/**
 * Strategy for determining the time range of backup recordings.
 * Defines predefined time ranges for backup operations.
 */
public enum BackupTimeRangeStrategy {

    /**
     * Backup recordings from the last hour (now - 1h → now)
     */
    LAST_HOUR,

    /**
     * Backup recordings from the last 24 hours (now - 24h → now)
     * This is the default strategy.
     */
    LAST_24_HOURS,

    /**
     * Backup recordings from the previous day (yesterday 00:00 → yesterday 23:59)
     */
    PREVIOUS_DAY
}