package com.kcn.hikvisionmanager.dto.xml.response;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import lombok.Data;

import java.util.List;

@Data
@JacksonXmlRootElement(localName = "TrackList", namespace = "http://www.hikvision.com/ver20/XMLSchema")
public class TrackListXml {

    @JacksonXmlProperty(localName = "Track")
    @JacksonXmlElementWrapper(useWrapping = false)
    private List<Track> tracks;

    @Data
    public static class Track {
        @JacksonXmlProperty(localName = "id")
        private String id;

        @JacksonXmlProperty(localName = "Channel")
        private String channel;

        @JacksonXmlProperty(localName = "Enable")
        private boolean enable;

        @JacksonXmlProperty(localName = "Description")
        private String description;

        @JacksonXmlProperty(localName = "DefaultRecordingMode")
        private String defaultRecordingMode;

        @JacksonXmlProperty(localName = "TrackSchedule")
        private TrackSchedule trackSchedule;

        @JacksonXmlProperty(localName = "CustomExtensionList")
        private CustomExtensionList customExtensionList;


        public RecordingType getRecordingType() {
            if (hasActiveSchedule()) {
                return RecordingType.SCHEDULE;
            }
            if ("CMR".equalsIgnoreCase(defaultRecordingMode)) {
                return RecordingType.CONTINUOUS;
            }
            if ("manual".equalsIgnoreCase(defaultRecordingMode)) {
                return RecordingType.MANUAL;
            }
            return RecordingType.UNKNOWN;
        }

        public boolean hasActiveSchedule() {
            if (trackSchedule == null ||
                    trackSchedule.getScheduleBlockList() == null ||
                    trackSchedule.getScheduleBlockList().getScheduleBlocks() == null) {
                return false;
            }

            return trackSchedule.getScheduleBlockList().getScheduleBlocks().stream()
                    .anyMatch(block -> block.getScheduleActions() != null &&
                            !block.getScheduleActions().isEmpty());
        }

        public String getScheduleDescription() {
            if (!hasActiveSchedule()) {
                return "No schedule";
            }
            if (is24x7Schedule()) {
                return "24/7 recording";
            } else {
                return "Custom schedule";
            }
        }

        private boolean is24x7Schedule() {
            return hasActiveSchedule() && "CMR".equalsIgnoreCase(defaultRecordingMode);
        }

        public String getCodec() {
            return extractFromDescription("codecType=");
        }

        public String getResolution() {
            return extractFromDescription("resolution=");
        }

        public String getFramerate() {
            return extractFromDescription("framerate=");
        }

        public String getBitrate() {
            return extractFromDescription("bitrate=");
        }

        private String extractFromDescription(String key) {
            if (description == null) return "Unknown";
            String[] parts = description.split(",");
            for (String part : parts) {
                if (part.trim().startsWith(key)) {
                    return part.trim().substring(key.length());
                }
            }
            return "Unknown";
        }
    }

    @Data
    public static class TrackSchedule {
        @JacksonXmlProperty(localName = "ScheduleBlockList")
        private ScheduleBlockList scheduleBlockList;
    }

    @Data
    public static class ScheduleBlockList {
        @JacksonXmlProperty(localName = "ScheduleBlock")
        @JacksonXmlElementWrapper(useWrapping = false)
        private List<ScheduleBlock> scheduleBlocks;
    }

    @Data
    public static class ScheduleBlock {
        @JacksonXmlProperty(isAttribute = true, localName = "ScheduleActionSize")
        private int scheduleActionSize;

        @JacksonXmlProperty(localName = "ScheduleAction")
        @JacksonXmlElementWrapper(useWrapping = false)
        private List<ScheduleAction> scheduleActions;
    }

    @Data
    public static class ScheduleAction {
        @JacksonXmlProperty(localName = "id")
        private String id;

        @JacksonXmlProperty(localName = "Actions")
        private Actions actions;
    }

    @Data
    public static class Actions {
        @JacksonXmlProperty(localName = "Record")
        private boolean record;
    }

    @Data
    public static class CustomExtensionList {
        @JacksonXmlProperty(localName = "CustomExtension")
        @JacksonXmlElementWrapper(useWrapping = false)
        private List<CustomExtension> customExtensions;
    }

    @Data
    public static class CustomExtension {
        @JacksonXmlProperty(localName = "enableSchedule")
        private boolean enableSchedule;
    }


}