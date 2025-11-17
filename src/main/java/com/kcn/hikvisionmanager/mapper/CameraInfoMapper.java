package com.kcn.hikvisionmanager.mapper;

import com.kcn.hikvisionmanager.dto.CameraInfoDTO;
import com.kcn.hikvisionmanager.dto.xml.response.DeviceInfoXml;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class CameraInfoMapper {

    public CameraInfoDTO toCameraInfoDTO(DeviceInfoXml deviceInfo) {
        if(deviceInfo == null) {
            return createEmptyInfoDTO();
        }

        return CameraInfoDTO.builder()
                .model(deviceInfo.getModel())
                .serialNumber(deviceInfo.getSerialNumber())
                .deviceDescription(deviceInfo.getDeviceDescription())
                .deviceLocation(deviceInfo.getDeviceLocation())
                .deviceName(deviceInfo.getDeviceName())
                .firmwareReleasedDate(deviceInfo.getFirmwareReleasedDate())
                .firmwareVersion(deviceInfo.getFirmwareVersion())
                .manufacturer(deviceInfo.getSystemContact().replace('.', ' '))
                .version(deviceInfo.getVersion())
                .build();


    }

    public CameraInfoDTO createEmptyInfoDTO() {
        return CameraInfoDTO.builder().build();
    }



}
