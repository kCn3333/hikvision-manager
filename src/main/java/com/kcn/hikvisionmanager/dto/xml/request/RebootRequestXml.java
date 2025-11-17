package com.kcn.hikvisionmanager.dto.xml.request;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import lombok.Data;

@Data
@JacksonXmlRootElement(localName = "rebootCamera")
public class RebootRequestXml {
    @JacksonXmlProperty(localName = "rebootType")
    private String rebootType = "reboot";
}
