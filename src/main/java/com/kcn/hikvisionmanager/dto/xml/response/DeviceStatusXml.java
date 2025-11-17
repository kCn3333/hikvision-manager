package com.kcn.hikvisionmanager.dto.xml.response;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import lombok.Data;

@Data
@JacksonXmlRootElement(localName = "DeviceStatus", namespace = "http://www.hikvision.com/ver20/XMLSchema")
public class DeviceStatusXml {

    @JacksonXmlProperty(localName = "currentDeviceTime")
    private String currentDeviceTime;

    @JacksonXmlProperty(localName = "deviceUpTime")
    private int deviceUpTime;

    @JacksonXmlProperty(localName = "CPUList")
    private CpuList cpuList;

    @JacksonXmlProperty(localName = "MemoryList")
    private MemoryList memoryList;

    @JacksonXmlProperty(localName = "totalRebootCount")
    private int totalRebootCount;

    @Data
    public static class CpuList {
        @JacksonXmlProperty(localName = "CPU")
        private Cpu cpu;
    }

    @Data
    public static class Cpu {
        @JacksonXmlProperty(localName = "cpuUtilization")
        private int cpuUtilization;
    }

    @Data
    public static class MemoryList {
        @JacksonXmlProperty(localName = "Memory")
        private Memory memory;
    }

    @Data
    public static class Memory {
        @JacksonXmlProperty(localName = "memoryUsage")
        private int memoryUsage;

        @JacksonXmlProperty(localName = "memoryAvailable")
        private int memoryAvailable;
    }
}