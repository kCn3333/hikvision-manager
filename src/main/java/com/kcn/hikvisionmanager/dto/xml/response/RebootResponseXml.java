package com.kcn.hikvisionmanager.dto.xml.response;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import lombok.Data;

@Data
@JacksonXmlRootElement(localName = "ResponseStatus", namespace = "http://www.hikvision.com/ver20/XMLSchema")
public class RebootResponseXml {

    @JacksonXmlProperty(localName = "requestURL")
    private String requestUrl;

    @JacksonXmlProperty(localName = "statusCode")
    private int statusCode;

    @JacksonXmlProperty(localName = "statusString")
    private String statusString;

    @JacksonXmlProperty(localName = "subStatusCode")
    private String subStatusCode;

    public boolean isSuccess() {
        return statusCode == 1 && "OK".equalsIgnoreCase(statusString);
    }
}