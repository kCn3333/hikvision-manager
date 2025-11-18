package com.kcn.hikvisionmanager.config;

import com.kcn.hikvisionmanager.domain.BackupTimeRangeStrategy;
import com.kcn.hikvisionmanager.exception.StorageException;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.format.DateTimeFormatter;

/**
 * Configuration for backup operations.
 * Manages backup storage locations, retention policies, and scheduling.
 */
@Getter
@Slf4j
@ConfigurationProperties(prefix = "backup")
public class BackupConfig {

    /**
     * Date format pattern for backup directory organization.
     * Format: yyyy-MM-dd (e.g., 2025-01-15)
     */
    public static final DateTimeFormatter DIR_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * Base directory where backups are stored permanently.
     * Example: ./backups or /app/data/backups (Docker)
     */
    private final Path baseDir;

    /**
     * Default retention period for backups in days.
     * Backups older than this will be automatically cleaned up.
     */
    private final int defaultRetentionDays;

    /**
     * Cron expression for scheduled backup execution.
     * Example: "0 0 3 * * *" (every day at 3:00 AM)
     */
    private final String defaultScheduleCron;

    /**
     * Maximum number of recordings to process in a single backup job
     */
    private final int maxRecordingsPerBackup;

    /**
     * Default strategy for determining backup time range
     */
    private final BackupTimeRangeStrategy defaultStrategy;

    /**
     * Maximum number of retry attempts for failed backup tasks.
     */
    private final int retryMaxAttempts;

    /**
     * Delay between retry attempts.
     */
    private final Duration retryDelay;

    /**
     * Constructs BackupConfig with validated parameters.
     *
     * @param baseDir Base directory for backups
     * @param defaultRetentionDays Number of days to retain backups
     * @param scheduleCron Cron expression for backup scheduling
     * @param maxRecordingsPerBackup max number of recordings for backup
     * @param defaultStrategy to determine backup time range
     * @param retryMaxAttempts Maximum retry attempts
     * @param retryDelay Delay between retries
     */
    public BackupConfig(Path baseDir,
                        int defaultRetentionDays,
                        String scheduleCron, int maxRecordingsPerBackup, BackupTimeRangeStrategy defaultStrategy,
                        int retryMaxAttempts,
                        Duration retryDelay) {
        this.baseDir = baseDir;
        this.defaultRetentionDays = defaultRetentionDays;
        this.defaultScheduleCron = scheduleCron;
        this.maxRecordingsPerBackup = maxRecordingsPerBackup;
        this.defaultStrategy = defaultStrategy;
        this.retryMaxAttempts = retryMaxAttempts;
        this.retryDelay = retryDelay;
    }

    /**
     * Validates configuration and initializes backup directories.
     * Ensures write permissions and directory accessibility.
     */
    @PostConstruct
    public void validateAndInitialize() {
        initDirectories();
        log.info("✅ BackupConfig initialized: baseDir={}, retention={} days, schedule={}",
                baseDir, defaultRetentionDays, defaultScheduleCron);
    }

    /**
     * Creates backup directories and verifies write permissions.
     *
     * @throws StorageException if directories cannot be created or are not writable
     */
    private void initDirectories() {
        try {
            // Create base directory if it doesn't exist
            Files.createDirectories(baseDir);

            // Verify write permissions
            if (!Files.isWritable(baseDir)) {
                throw new StorageException(
                        "Backup directory is not writable: " + baseDir.toAbsolutePath());
            }

            log.info("✅ Backup directory initialized and writable: {}", baseDir.toAbsolutePath());

        } catch (Exception e) {
            log.error("❌ Failed to initialize backup directory: {}", baseDir, e);
            throw new StorageException("Failed to initialize backup directories", e);
        }
    }
}