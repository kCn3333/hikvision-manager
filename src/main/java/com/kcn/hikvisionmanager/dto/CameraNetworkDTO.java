package com.kcn.hikvisionmanager.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CameraNetworkDTO {

    private String ipAddress;
    private String macAddress;
    private String subnetMask;
    private String defaultGateway;
    private String dnsServer;
    private String speed;
    private String mtu;
    private String duplex;

}
