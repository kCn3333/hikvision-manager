package com.kcn.hikvisionmanager.mapper;

import com.kcn.hikvisionmanager.dto.CameraStorageDTO;
import com.kcn.hikvisionmanager.dto.xml.response.DeviceStorageInfoXml;
import org.springframework.stereotype.Component;

@Component
public class CameraStorageMapper {

    public CameraStorageDTO toCameraStorageDTO(DeviceStorageInfoXml deviceStorageInfo) {

        return CameraStorageDTO.builder()
                .id(deviceStorageInfo.getFirstHdd().getId())
                .name(deviceStorageInfo.getFirstHdd().getHddName())
                .path(deviceStorageInfo.getFirstHdd().getHddPath())
                .type(deviceStorageInfo.getFirstHdd().getHddType())
                .status(deviceStorageInfo.getHddStatus())
                .capacity(deviceStorageInfo.getFormattedCapacity())
                .usage(deviceStorageInfo.getHddUsagePercentage()+" %")
                .formatType(deviceStorageInfo.getFirstHdd().getFormatType())
                .mountTypes(deviceStorageInfo.getSupportedMountTypes())
                .authentications(deviceStorageInfo.getAuthenticationTypes())
                .build();

    }


}
