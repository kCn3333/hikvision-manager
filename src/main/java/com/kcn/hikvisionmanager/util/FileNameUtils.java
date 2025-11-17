package com.kcn.hikvisionmanager.util;

import com.kcn.hikvisionmanager.dto.RecordingItemDTO;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.stream.Stream;

import static com.kcn.hikvisionmanager.config.BackupConfig.DIR_FORMAT;

@UtilityClass
@Slf4j
public final class FileNameUtils {

    private static final DateTimeFormatter FILE_NAME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

    /**
     * Generate file name: recording_2025-10-30_15-55-28_2025-10-30_16-05-54.mp4
     */
    public static String generateFileName(RecordingItemDTO recording) {
        String start = recording.getStartTime().format(FILE_NAME_FORMAT);
        String end = recording.getEndTime().format(FILE_NAME_FORMAT);
        return String.format("recording_%s_%s.mp4", start, end);
    }

    /**
     * Build backup directory path: /backups/2025-11-01/
     */
    public static String buildBackupDirectory(String basePath, LocalDateTime date) {
        String dateDir = date.format(DIR_FORMAT);
        return Paths.get(basePath, dateDir).toString();
    }

    /**
     * Delete directory recursively
     */
    public static void deleteDirectory(Path directory) throws IOException {
        if (Files.exists(directory)) {
            try (Stream<Path> walk = Files.walk(directory)) {
                walk.sorted((a, b) -> -a.compareTo(b))  // Reverse order (files before dirs)
                        .forEach(path -> {
                            try {
                                Files.delete(path);
                            } catch (IOException e) {
                                log.warn("Failed to delete {}: {}", path, e.getMessage());
                            }
                        });
            }
        }
    }

}