package com.kcn.hikvisionmanager.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CameraInfoDTO {

    private String version;
    private String deviceName;
    private String deviceDescription;
    private String manufacturer;
    private String deviceLocation;
    private String model;
    private String serialNumber;
    private String firmwareVersion;
    private String firmwareReleasedDate;
}
