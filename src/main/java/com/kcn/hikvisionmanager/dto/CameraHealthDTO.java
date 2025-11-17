package com.kcn.hikvisionmanager.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class CameraHealthDTO {
    private boolean online;
    private int cpuUsage;
    private int memoryUsage;
    private int uptimeMinutes;
    private String status;
    private String currentDeviceTime;

    public String getFormattedUptime() {
        if (uptimeMinutes < 60) return uptimeMinutes+" minutes";
        if ((uptimeMinutes/60) < 24) return  (uptimeMinutes/60)+ " hours, "+(uptimeMinutes%60)+" minutes";
        return ((uptimeMinutes/60) / 24) + " days, " + (uptimeMinutes/60)%24 + " hours, " + (uptimeMinutes%3600) + " minutes";
    }
}
