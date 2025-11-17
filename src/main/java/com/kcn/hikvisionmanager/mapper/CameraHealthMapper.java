package com.kcn.hikvisionmanager.mapper;

import com.kcn.hikvisionmanager.dto.CameraHealthDTO;
import com.kcn.hikvisionmanager.dto.xml.response.DeviceStatusXml;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

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
                .uptimeMinutes(deviceStatus.getDeviceUpTime() / 60)
                .status("Connected")
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
}