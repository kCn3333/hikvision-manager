package com.kcn.hikvisionmanager.dto.xml.response;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import lombok.Data;

@Data
@JacksonXmlRootElement(localName = "DeviceInfo", namespace = "http://www.hikvision.com/ver20/XMLSchema")
public class DeviceInfoXml {

    @JacksonXmlProperty(isAttribute = true, localName = "version")
    private String version;

    @JacksonXmlProperty(localName = "deviceName")
    private String deviceName;

    @JacksonXmlProperty(localName = "deviceID")
    private String deviceId;

    @JacksonXmlProperty(localName = "deviceDescription")
    private String deviceDescription;

    @JacksonXmlProperty(localName = "deviceLocation")
    private String deviceLocation;

    @JacksonXmlProperty(localName = "systemContact")
    private String systemContact;

    @JacksonXmlProperty(localName = "model")
    private String model;

    @JacksonXmlProperty(localName = "serialNumber")
    private String serialNumber;

    @JacksonXmlProperty(localName = "macAddress")
    private String macAddress;

    @JacksonXmlProperty(localName = "firmwareVersion")
    private String firmwareVersion;

    @JacksonXmlProperty(localName = "firmwareReleasedDate")
    private String firmwareReleasedDate;

    @JacksonXmlProperty(localName = "encoderVersion")
    private String encoderVersion;

    @JacksonXmlProperty(localName = "encoderReleasedDate")
    private String encoderReleasedDate;

    @JacksonXmlProperty(localName = "bootVersion")
    private String bootVersion;

    @JacksonXmlProperty(localName = "bootReleasedDate")
    private String bootReleasedDate;

    @JacksonXmlProperty(localName = "hardwareVersion")
    private String hardwareVersion;

    @JacksonXmlProperty(localName = "deviceType")
    private String deviceType;

    @JacksonXmlProperty(localName = "telecontrolID")
    private String telecontrolId;

    @JacksonXmlProperty(localName = "supportBeep")
    private boolean supportBeep;

    @JacksonXmlProperty(localName = "supportVideoLoss")
    private boolean supportVideoLoss;

    @JacksonXmlProperty(localName = "firmwareVersionInfo")
    private String firmwareVersionInfo;
}