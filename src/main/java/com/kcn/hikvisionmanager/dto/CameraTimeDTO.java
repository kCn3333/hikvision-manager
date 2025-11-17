package com.kcn.hikvisionmanager.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CameraTimeDTO {
    private String timeMode;
    private String localTime;
    private String formattedLocalTime;
    private String timeZone;
    private String formattedTimeZone;
    private boolean ntpEnabled;
    private String statusMessage;
}
