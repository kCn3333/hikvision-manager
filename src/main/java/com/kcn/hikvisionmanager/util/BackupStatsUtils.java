package com.kcn.hikvisionmanager.util;

import lombok.experimental.UtilityClass;

@UtilityClass
public final class BackupStatsUtils {

    /**
     * Calculates backup success rate as percentage (0–100).
     */
    public static double calculateSuccessRate(long completed, long total) {
        if (total == 0) return 0.0;
        return (completed * 100.0) / total;
    }

    /**
     * Formats byte size into human-readable form.
     */
    public static String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        double kb = bytes / 1024.0;
        if (kb < 1024) return String.format("%.1f KB", kb);
        double mb = kb / 1024.0;
        if (mb < 1024) return String.format("%.1f MB", mb);
        double gb = mb / 1024.0;
        return String.format("%.2f GB", gb);
    }

    /**
     * Formats duration in minutes into readable hours/minutes.
     */
    public static String formatDuration(long minutes) {
        if (minutes < 60) return minutes + " minutes";
        long hours = minutes / 60;
        long mins = minutes % 60;
        return String.format("%d hours %d minutes", hours, mins);
    }
}
