package com.kcn.hikvisionmanager.dto.xml.request;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import lombok.Data;

@Data
@JacksonXmlRootElement(localName = "downloadRequest")
public class RecordingDownloadRequestXml {
    @JacksonXmlProperty(localName = "playbackURI")
    private String playbackUri;

    public RecordingDownloadRequestXml() {}

    public RecordingDownloadRequestXml(String playbackUri) {
        this.playbackUri = playbackUri;
    }
}