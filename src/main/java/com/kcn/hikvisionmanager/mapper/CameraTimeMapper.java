package com.kcn.hikvisionmanager.mapper;

import com.kcn.hikvisionmanager.dto.CameraTimeDTO;
import com.kcn.hikvisionmanager.dto.xml.response.DeviceTimeXml;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@Slf4j
@Component
public class CameraTimeMapper {

    public CameraTimeDTO toCameraTimeDTO(DeviceTimeXml deviceTime) {
        if (deviceTime == null) {
            return createEmptyTimeDTO();
        }

        return CameraTimeDTO.builder()
                .timeMode(deviceTime.getTimeMode())
                .localTime(deviceTime.getLocalTime())
                .formattedLocalTime(formatLocalTime(deviceTime.getLocalTime()))
                .timeZone(deviceTime.getTimeZone())
                .formattedTimeZone(deviceTime.getFormattedTimeZone())
                .ntpEnabled(deviceTime.isNtpEnabled())
                .statusMessage(buildStatusMessage(deviceTime))
                .build();
    }

    private String formatLocalTime(String localTime) {
        if (localTime == null) return "Unknown";

        try {
            LocalDateTime dateTime = LocalDateTime.parse(localTime, DateTimeFormatter.ISO_DATE_TIME);
            return dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        } catch (DateTimeParseException e) {
            log.warn("Failed to parse local time: {}", localTime);
            return localTime;
        }
    }

    private String buildStatusMessage(DeviceTimeXml deviceTime) {
        if (deviceTime.isNtpEnabled()) {
            return String.format("NTP Sync (Interval: %dh )", deviceTime.getSatelliteInterval()/60);
        } else if (deviceTime.isManualMode()) {
            return "Manual Time Setting";
        } else {
            return "Unknown Time Mode";
        }
    }

    public CameraTimeDTO createEmptyTimeDTO() {
        return CameraTimeDTO.builder()
                .timeMode("Offline")
                .localTime("Unknown")
                .formattedLocalTime("Unknown")
                .timeZone("Offline")
                .formattedTimeZone("Offline")
                .ntpEnabled(false)
                .statusMessage("Camera offline - time data unavailable")
                .build();
    }
}
