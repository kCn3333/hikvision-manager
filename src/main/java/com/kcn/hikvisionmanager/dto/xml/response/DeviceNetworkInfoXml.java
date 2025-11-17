package com.kcn.hikvisionmanager.dto.xml.response;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import lombok.Data;

@Data
@JacksonXmlRootElement(localName = "NetworkInterface", namespace = "http://www.hikvision.com/ver20/XMLSchema")
public class DeviceNetworkInfoXml {

    @JacksonXmlProperty(localName = "id")
    private int id;

    @JacksonXmlProperty(localName = "IPAddress")
    private IPAddress ipAddress;

    @JacksonXmlProperty(localName = "Link")
    private Link link;

    @Data
    public static class IPAddress {

        @JacksonXmlProperty(localName = "addressingType")
        private String addressingType;

        @JacksonXmlProperty(localName = "ipAddress")
        private String ipAddress;

        @JacksonXmlProperty(localName = "subnetMask")
        private String subnetMask;

        @JacksonXmlProperty(localName = "DefaultGateway")
        private DefaultGateway defaultGateway;

        @JacksonXmlProperty(localName = "PrimaryDNS")
        private DnsServer primaryDns;

        @JacksonXmlProperty(localName = "SecondaryDNS")
        private DnsServer secondaryDns;

    }

    @Data
    public static class DefaultGateway {
        @JacksonXmlProperty(localName = "ipAddress")
        private String ipAddress;
    }

    @Data
    public static class DnsServer {
        @JacksonXmlProperty(localName = "ipAddress")
        private String ipAddress;
    }


    @Data
    public static class Link {
        @JacksonXmlProperty(localName = "MACAddress")
        private String macAddress;

        @JacksonXmlProperty(localName = "speed")
        private String speed;

        @JacksonXmlProperty(localName = "duplex")
        private String duplex;

        @JacksonXmlProperty(localName = "MTU")
        private String mtu;
    }

//    // Business logic methods
//    public String getMacAddress() {
//        return link != null ? link.getMacAddress() : null;
//    }
//
//    public String getNetworkType() {
//        return ipAddress != null ? ipAddress.getAddressingType() : null;
//    }
//
//    public boolean isDhcpEnabled() {
//        return "dhcp".equalsIgnoreCase(getNetworkType());
//    }
//
//    public String getGateway() {
//        return ipAddress != null && ipAddress.getDefaultGateway() != null ?
//                ipAddress.getDefaultGateway().getIpAddress() : null;
//    }
//
//    public String getDnsServers() {
//        if (ipAddress == null) return "";
//
//        StringBuilder dns = new StringBuilder();
//        if (ipAddress.getPrimaryDns() != null) {
//            dns.append(ipAddress.getPrimaryDns().getIpAddress());
//        }
//        if (ipAddress.getSecondaryDns() != null &&
//                !"0.0.0.0".equals(ipAddress.getSecondaryDns().getIpAddress())) {
//            dns.append(", ").append(ipAddress.getSecondaryDns().getIpAddress());
//        }
//        return dns.toString();
//    }
}