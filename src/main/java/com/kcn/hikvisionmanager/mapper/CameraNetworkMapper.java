package com.kcn.hikvisionmanager.mapper;


import com.kcn.hikvisionmanager.dto.CameraNetworkDTO;
import com.kcn.hikvisionmanager.dto.xml.response.DeviceNetworkInfoXml;
import org.springframework.stereotype.Component;


@Component
public class CameraNetworkMapper {

    public CameraNetworkDTO toCameraNetworkDTO(DeviceNetworkInfoXml deviceNetworkInfo) {

        return CameraNetworkDTO.builder()
                .ipAddress(deviceNetworkInfo.getIpAddress().getIpAddress())
                .macAddress(deviceNetworkInfo.getLink().getMacAddress())
                .subnetMask(deviceNetworkInfo.getIpAddress().getSubnetMask())
                .defaultGateway(deviceNetworkInfo.getIpAddress().getDefaultGateway().getIpAddress())
                .dnsServer(deviceNetworkInfo.getIpAddress().getPrimaryDns().getIpAddress())
                .speed(deviceNetworkInfo.getLink().getSpeed()+" Mbps")
                .mtu(deviceNetworkInfo.getLink().getMtu())
                .duplex(deviceNetworkInfo.getLink().getDuplex())
                .build();
    }

}
