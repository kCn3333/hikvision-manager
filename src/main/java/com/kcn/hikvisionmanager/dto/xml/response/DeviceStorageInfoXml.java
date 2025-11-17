package com.kcn.hikvisionmanager.dto.xml.response;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Data
@Slf4j
@JacksonXmlRootElement(localName = "storage", namespace = "http://www.hikvision.com/ver20/XMLSchema")
public class DeviceStorageInfoXml {

    @JacksonXmlProperty(localName = "hddList")
    private HddList hddList;

    @JacksonXmlProperty(localName = "nasList")
    private NasList nasList;

    @Data
    public static class HddList {

        @JacksonXmlProperty(localName = "hdd")
        @JacksonXmlElementWrapper(useWrapping = false)
        private List<Hdd> hdds;
    }

    @Data
    public static class Hdd {
        @JacksonXmlProperty(localName = "id")
        private int id;

        @JacksonXmlProperty(localName = "hddName")
        private String hddName;

        @JacksonXmlProperty(localName = "hddPath")
        private String hddPath;

        @JacksonXmlProperty(localName = "hddType")
        private String hddType;

        @JacksonXmlProperty(localName = "status")
        private String status;

        @JacksonXmlProperty(localName = "capacity")
        private long capacity; // w MB

        @JacksonXmlProperty(localName = "freeSpace")
        private long freeSpace; // w MB

        @JacksonXmlProperty(localName = "property")
        private String property;

        @JacksonXmlProperty(localName = "formatType")
        private String formatType;
    }

    @Data
    public static class NasList {
        @JacksonXmlProperty(localName = "supportMountType")
        private MountType supportMountType;

        @JacksonXmlProperty(localName = "authentication")
        private Authentication authentication;
    }

    @Data
    public static class MountType {
        @JacksonXmlProperty(isAttribute = true, localName = "opt")
        private String options;
    }

    @Data
    public static class Authentication {
        @JacksonXmlProperty(isAttribute = true, localName = "opt")
        private String options;
    }

    // Business logic methods
    public boolean hasHdd() {
        return hddList != null && hddList.getHdds() != null && !hddList.getHdds().isEmpty();
    }

    public Hdd getFirstHdd() {
        return hasHdd() ? hddList.getHdds().get(0) : null;
    }

    public Double getHddUsagePercentage() {
        Hdd hdd = getFirstHdd();
        if (hdd == null || hdd.getCapacity() == 0) return 0.0;

        long usedSpace = hdd.getCapacity() - hdd.getFreeSpace();
        return ((double) usedSpace / hdd.getCapacity() * 100);
    }

    public String getHddStatus() {
        Hdd hdd = getFirstHdd();
        return hdd != null ? hdd.getStatus() : "no_hdd";
    }

    public boolean isHddHealthy() {
        return "ok".equalsIgnoreCase(getHddStatus());
    }

    public String getFormattedCapacity() {
        Hdd hdd = getFirstHdd();
        if (hdd == null) return "No HDD";

        log.debug("HDD Capacity: {} MB, Free: {} MB", hdd.getCapacity(), hdd.getFreeSpace());

        double capacityGB = hdd.getCapacity() / 1024.0;
        double freeSpaceGB = hdd.getFreeSpace() / 1024.0;
        double usagePercent = getHddUsagePercentage();

        log.debug("Calculated - Capacity: {} GB, Free: {} GB, Usage: {}%",
                capacityGB, freeSpaceGB, usagePercent);

        return String.format("%.1f GB / %.1f GB (%.1f%%)",
                capacityGB - freeSpaceGB, capacityGB, usagePercent);
    }

    public List<String> getSupportedMountTypes() {
        if (nasList != null && nasList.getSupportMountType() != null) {
            String options = nasList.getSupportMountType().getOptions();
            return options != null ? List.of(options.split(",")) : List.of();
        }
        return List.of();
    }

    public List<String> getAuthenticationTypes() {
        if (nasList != null && nasList.getAuthentication() != null) {
            String options = nasList.getAuthentication().getOptions();
            return options != null ? List.of(options.split(",")) : List.of();
        }
        return List.of();
    }
}