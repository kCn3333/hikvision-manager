package com.kcn.hikvisionmanager.dto.xml.response;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import lombok.Data;

import java.util.List;

@Data
@JacksonXmlRootElement(localName = "CMSearchResult", namespace = "http://www.hikvision.com/ver20/XMLSchema")
public class RecordingSearchResponseXml {

    @JacksonXmlProperty(localName = "searchID")
    private String searchId;

    @JacksonXmlProperty(localName = "responseStatus")
    private boolean responseStatus;

    @JacksonXmlProperty(localName = "responseStatusStrg")
    private String responseStatusString;

    @JacksonXmlProperty(localName = "numOfMatches")
    private int numOfMatches;

    @JacksonXmlProperty(localName = "matchList")
    private MatchList matchList;

    @Data
    public static class MatchList {
        @JacksonXmlProperty(localName = "searchMatchItem")
        @JacksonXmlElementWrapper(useWrapping = false)
        private List<SearchMatchItem> searchMatchItems;
    }

    @Data
    public static class SearchMatchItem {
        @JacksonXmlProperty(localName = "sourceID")
        private String sourceId;

        @JacksonXmlProperty(localName = "trackID")
        private String trackId;

        @JacksonXmlProperty(localName = "timeSpan")
        private TimeSpan timeSpan;

        @JacksonXmlProperty(localName = "mediaSegmentDescriptor")
        private MediaSegmentDescriptor mediaSegmentDescriptor;

        @Data
        public static class TimeSpan {
            @JacksonXmlProperty(localName = "startTime")
            private String startTime;

            @JacksonXmlProperty(localName = "endTime")
            private String endTime;
        }

        @Data
        public static class MediaSegmentDescriptor {
            @JacksonXmlProperty(localName = "contentType")
            private String contentType;

            @JacksonXmlProperty(localName = "codecType")
            private String codecType;

            @JacksonXmlProperty(localName = "playbackURI")
            private String playbackUri;
        }
    }
}