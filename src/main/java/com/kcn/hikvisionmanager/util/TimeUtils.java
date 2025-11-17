package com.kcn.hikvisionmanager.util;

import lombok.experimental.UtilityClass;

import java.time.*;
import java.time.format.DateTimeFormatter;


@UtilityClass
public class TimeUtils {
    private static final ZoneId LOCAL_ZONE = ZoneId.systemDefault();


    // Format dla kamery (UTC)
    private static final DateTimeFormatter CAMERA_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");

    // Format dla UI (ISO Local DateTime) - TAKI JAK MASZ
    private static final DateTimeFormatter UI_FORMAT =
            DateTimeFormatter.ISO_LOCAL_DATE_TIME; // "yyyy-MM-dd'T'HH:mm"

    /**
     * UI LocalDateTime → UTC String dla kamery
     */
    public static String localToCameraUtc(LocalDateTime localDateTime) {
        return localDateTime.atZone(LOCAL_ZONE)
                .withZoneSameInstant(ZoneOffset.UTC)
                .format(CAMERA_FORMAT);
    }

    /**
     * UTC String z kamery → LocalDateTime dla UI
     */
    public static LocalDateTime cameraUtcToLocal(String utcString) {
        OffsetDateTime utcTime = OffsetDateTime.parse(utcString);
        return utcTime.atZoneSameInstant(LOCAL_ZONE).toLocalDateTime();
    }

    public static LocalDateTime cameraUtcToLocal(LocalDateTime utcTime) {
        if (utcTime == null) return null;
        return utcTime.atOffset(ZoneOffset.UTC)
                .atZoneSameInstant(LOCAL_ZONE)
                .toLocalDateTime();
    }

    /**
     * Formatowanie dla wyświetlania w UI (jeśli potrzebujesz)
     */
    public static String formatForDisplay(LocalDateTime localDateTime) {
        return localDateTime.format(UI_FORMAT); // Zwraca "2025-10-29T16:00"
    }

    /**
     * Parsowanie z UI (jeśli potrzebujesz)
     */
    public static LocalDateTime parseFromUi(String uiDateTimeString) {
        return LocalDateTime.parse(uiDateTimeString, UI_FORMAT);
    }
}