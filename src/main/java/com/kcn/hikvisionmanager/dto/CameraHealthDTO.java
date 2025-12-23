package com.kcn.hikvisionmanager.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Duration;

@Data
@Builder
public class CameraHealthDTO {
    private boolean online;
    private int cpuUsage;
    private int memoryUsage;
    private int uptimeMinutes;
    private String status;
    private String currentDeviceTime;

    private String formattedUptime;
}


