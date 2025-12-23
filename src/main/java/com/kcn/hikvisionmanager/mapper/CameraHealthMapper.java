package com.kcn.hikvisionmanager.mapper;

import com.kcn.hikvisionmanager.dto.CameraHealthDTO;
import com.kcn.hikvisionmanager.dto.xml.response.DeviceStatusXml;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Component
public class CameraHealthMapper {

    public CameraHealthDTO toCameraHealthDTO(DeviceStatusXml deviceStatus) {
        if (deviceStatus == null) {
            return createOfflineStatus();
        }

        return CameraHealthDTO.builder()
                .online(true)
                .cpuUsage(deviceStatus.getCpuList().getCpu().getCpuUtilization())
                .memoryUsage(deviceStatus.getMemoryList().getMemory().getMemoryUsage())
                .uptimeMinutes(deviceStatus.getDeviceUpTime()/60)
                .status("Connected")
                .formattedUptime(getFormattedUptime(deviceStatus.getDeviceUpTime()/60))
                .currentDeviceTime(parseDeviceTime(deviceStatus.getCurrentDeviceTime()))
                .build();
    }

    private CameraHealthDTO createOfflineStatus() {
        return CameraHealthDTO.builder()
                .online(false)
                .cpuUsage(-1)
                .memoryUsage(-1)
                .uptimeMinutes(0)
                .status("Camera offline or not responding")
                .build();
    }

    private String parseDeviceTime(String deviceTime) {
        try {
            LocalDateTime dateTime = LocalDateTime.parse(deviceTime, DateTimeFormatter.ISO_DATE_TIME);
            return dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        } catch (Exception e) {
            log.warn("Failed to parse device time: {}", deviceTime);
            return null;
        }
    }

    public String getFormattedUptime(int uptimeMinutes) {
        Duration duration = Duration.ofMinutes(uptimeMinutes);

        long days = duration.toDays();
        long hours = duration.toHoursPart();
        long minutes = duration.toMinutesPart();

        if (days == 0) {
            if (hours == 0) {
                return minutes + " minutes";
            }
            return hours + " hours, " + minutes + " minutes";
        }
        return days + " days, " + hours + " hours, " + minutes + " minutes";
    }

}