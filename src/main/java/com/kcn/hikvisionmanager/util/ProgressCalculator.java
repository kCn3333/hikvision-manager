package com.kcn.hikvisionmanager.util;


import lombok.Builder;
import lombok.Data;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

/**
 * Calculate download progress based on time and file size estimation
 */
@Slf4j
@UtilityClass
public class ProgressCalculator {


    @Data
    @Builder
    public static class ProgressInfo {
        private int progressPercent;
        private String currentTime;
        private long downloadedBytes;
        private long totalBytes;
        private String downloadedSize;    // "112 MB"
        private String totalSize;         // "249 MB"
        private String eta;
    }

    /**
     * Calculate progress from FFmpeg time output and expected file size
     *
     * @param currentTimeStr   Current time from FFmpeg (e.g., "00:04:42")
     * @param totalDuration    Total duration (e.g., "10:26")
     * @param expectedFileSize Expected file size from search (e.g., "248,6 MB")
     * @return Progress information with size estimation
     */
    public static ProgressInfo calculate(String currentTimeStr, String totalDuration, String expectedFileSize) {
        try {
            long currentSeconds = parseTimeToSeconds(currentTimeStr);
            long totalSeconds = parseTimeToSeconds(totalDuration);

            if (totalSeconds == 0) {
                return createUnknownProgress(currentTimeStr, expectedFileSize);
            }

            // Calculate percentage based on time
            int progress = (int) ((currentSeconds * 100) / totalSeconds);
            progress = Math.min(progress, 100); // Cap at 100%

            // Parse expected file size to bytes
            long totalBytes = parseFileSize(expectedFileSize);

            // Estimate downloaded bytes based on time progress
            long downloadedBytes = (long) (totalBytes * (currentSeconds / (double) totalSeconds));

            // Calculate ETA
            long remainingSeconds = totalSeconds - currentSeconds;
            String eta = formatEta(remainingSeconds);

            return ProgressInfo.builder()
                    .progressPercent(progress)
                    .currentTime(currentTimeStr)
                    .downloadedBytes(downloadedBytes)
                    .totalBytes(totalBytes)
                    .downloadedSize(formatBytes(downloadedBytes))
                    .totalSize(formatBytes(totalBytes))
                    .eta(eta)
                    .build();

        } catch (Exception e) {
            log.warn("Failed to calculate progress: {}", e.getMessage());
            return createUnknownProgress(currentTimeStr, expectedFileSize);
        }
    }

    /**
     * Create progress info when calculation fails
     */
    private static ProgressInfo createUnknownProgress(String currentTime, String expectedFileSize) {
        long totalBytes = parseFileSize(expectedFileSize);
        return ProgressInfo.builder()
                .progressPercent(0)
                .currentTime(currentTime)
                .downloadedBytes(0)
                .totalBytes(totalBytes)
                .downloadedSize("0 MB")
                .totalSize(formatBytes(totalBytes))
                .eta("Unknown")
                .build();
    }

    /**
     * Parse time string to seconds
     * Supports formats: "MM:SS" or "HH:MM:SS" or "HH:MM:SS.MS"
     */
    private static long parseTimeToSeconds(String timeStr) {
        // Remove milliseconds if present
        timeStr = timeStr.split("\\.")[0].trim();

        String[] parts = timeStr.split(":");

        if (parts.length == 2) {
            // MM:SS
            return Long.parseLong(parts[0]) * 60 + Long.parseLong(parts[1]);
        } else if (parts.length == 3) {
            // HH:MM:SS
            return Long.parseLong(parts[0]) * 3600 +
                    Long.parseLong(parts[1]) * 60 +
                    Long.parseLong(parts[2]);
        }

        return 0;
    }

    /**
     * Parse file size string to bytes
     * Supports formats: "248,6 MB", "248.6 MB", "1,2 GB", "512 KB"
     */
    public static long parseFileSize(String fileSize) {
        try {
            // Normalize: "248,6 MB" -> "248.6 MB"
            String normalized = fileSize.replace(",", ".").trim();

            // Extract number: "248.6 MB" -> "248.6"
            String numberStr = normalized.replaceAll("[^0-9.]", "").trim();
            double value = Double.parseDouble(numberStr);

            // Detect unit
            String upper = normalized.toUpperCase();
            if (upper.contains("GB")) {
                return (long) (value * 1024 * 1024 * 1024);
            } else if (upper.contains("MB")) {
                return (long) (value * 1024 * 1024);
            } else if (upper.contains("KB")) {
                return (long) (value * 1024);
            } else {
                // Assume bytes
                return (long) value;
            }
        } catch (Exception e) {
            log.warn("Failed to parse file size: {}", fileSize, e);
            return 0;
        }
    }

    /**
     * Format bytes to human-readable size
     * Examples: "112 MB", "1.2 GB", "512 KB"
     */
    public static String formatBytes(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }

        double kb = bytes / 1024.0;
        if (kb < 1024) {
            return String.format("%.0f KB", kb);
        }

        double mb = kb / 1024.0;
        if (mb < 1024) {
            return String.format("%.0f MB", mb);
        }

        double gb = mb / 1024.0;
        return String.format("%.1f GB", gb);
    }

    /**
     * Format remaining seconds as ETA string
     * Examples: "30s", "2m 30s", "1h 15m"
     */
    private static String formatEta(long seconds) {
        if (seconds < 0) {
            return "Unknown";
        }

        if (seconds < 60) {
            return seconds + "s";
        } else if (seconds < 3600) {
            long minutes = seconds / 60;
            long secs = seconds % 60;
            return String.format("%dm %ds", minutes, secs);
        } else {
            long hours = seconds / 3600;
            long minutes = (seconds % 3600) / 60;
            return String.format("%dh %dm", hours, minutes);
        }
    }
}