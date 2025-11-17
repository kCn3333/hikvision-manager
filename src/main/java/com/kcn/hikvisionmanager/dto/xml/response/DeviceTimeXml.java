package com.kcn.hikvisionmanager.dto.xml.response;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import lombok.Data;

@Data
@JacksonXmlRootElement(localName = "Time", namespace = "http://www.hikvision.com/ver20/XMLSchema")
public class DeviceTimeXml {

    @JacksonXmlProperty(localName = "timeMode")
    private String timeMode;

    @JacksonXmlProperty(localName = "localTime")
    private String localTime;

    @JacksonXmlProperty(localName = "timeZone")
    private String timeZone;

    @JacksonXmlProperty(localName = "satelliteInterval")
    private int satelliteInterval;

    @JacksonXmlProperty(localName = "carrierInterval")
    private int carrierInterval;

    public boolean isNtpEnabled() {
        return "NTP".equalsIgnoreCase(timeMode);
    }

    public String getFormattedTimeZone() {
        if (timeZone == null) return "Unknown";
        return timeZone.replace("CST", "UTC");
    }

    public boolean isManualMode() {
        return "manual".equalsIgnoreCase(timeMode);
    }
}
