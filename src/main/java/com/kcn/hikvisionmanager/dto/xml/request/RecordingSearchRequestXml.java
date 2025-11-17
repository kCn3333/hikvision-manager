package com.kcn.hikvisionmanager.dto.xml.request;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import lombok.Data;

@Data
@JacksonXmlRootElement(localName = "CMSearchDescription")
public class RecordingSearchRequestXml {

    @JacksonXmlProperty(localName = "searchID")
    private String searchId;

    @JacksonXmlProperty(localName = "trackIDList")
    private TrackIdList trackIdList;

    @JacksonXmlProperty(localName = "timeSpanList")
    private TimeSpanList timeSpanList;

    @JacksonXmlProperty(localName = "maxResults")
    private int maxResults;

    @JacksonXmlProperty(localName = "searchResultPosition")
    private int searchResultPosition;

    @Data
    public static class TrackIdList {
        @JacksonXmlProperty(localName = "trackID")
        private String trackId;
    }

    @Data
    public static class TimeSpanList {
        @JacksonXmlProperty(localName = "timeSpan")
        private TimeSpan timeSpan;
    }

    @Data
    public static class TimeSpan {
        @JacksonXmlProperty(localName = "startTime")
        private String startTime; // UTC format: 2025-10-29T14:42:19Z

        @JacksonXmlProperty(localName = "endTime")
        private String endTime; // UTC format: 2025-10-29T15:19:41Z
    }
}