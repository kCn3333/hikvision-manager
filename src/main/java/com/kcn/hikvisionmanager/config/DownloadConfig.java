package com.kcn.hikvisionmanager.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Configuration for video recording download operations.
 * Manages download storage, retention policies, and concurrency limits.
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "recording.download")
public class DownloadConfig {

    /**
     * Download method: "http" or "ffmpeg".
     * HTTP method uses camera's native download API.
     * FFmpeg method uses stream recording.
     */
    private String method;

    /**
     * Directory where downloaded recordings are temporarily stored.
     * Default: /tmp/recordings/manual
     *
     * Note: In Docker deployments, this path should be mapped to a volume
     * for persistent storage and host access.
     */
    private String directory;

    /**
     * Maximum retention time for downloaded files in hours.
     * Files older than this will be automatically cleaned up.
     * Default: 24 hours
     */
    private int maxRetentionHours = 24;

    /**
     * Cron expression for cleanup task execution.
     * Default: Every hour (0 0 * * * *)
     */
    private String cleanupCron = "0 0 * * * *";

    /**
     * Maximum concurrent downloads from camera.
     * Limited by camera hardware capability.
     * Default: 1 (Hikvision camera limitation)
     */
    private int maxConcurrentCamera = 1;

    /**
     * When enabled, keeps only the last completed download for manual operations.
     * Prevents storage accumulation from repeated downloads.
     * Default: true
     */
    private boolean keepLastCompletedOnly = true;

    /**
     * Timeout for single download operation in minutes.
     * Downloads exceeding this duration will be terminated.
     * Default: 30 minutes
     */
    private int timeoutMinutes = 30;

    /**
     * Cache TTL for download job metadata in hours.
     * Should match or exceed maxRetentionHours.
     * Default: 24 hours
     */
    private int cacheTtlHours = 24;

    /**
     * Returns download directory as a Path object.
     *
     * @return Path to download directory
     */
    public Path getDownloadPath() {
        return Paths.get(directory);
    }

    /**
     * Checks if HTTP download method is configured.
     *
     * @return true if method is "http", false otherwise
     */
    public boolean isHttpDownload() {
        return "http".equals(method);
    }
}