package com.kcn.hikvisionmanager.util;

import lombok.experimental.UtilityClass;

import java.time.*;
import java.time.format.DateTimeFormatter;


@UtilityClass
public class TimeUtils {
    private static final ZoneId LOCAL_ZONE = ZoneId.systemDefault();


    // Format (UTC)
    private static final DateTimeFormatter CAMERA_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");

    // Format dla UI (ISO Local DateTime)
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

}