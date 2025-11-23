package com.kcn.hikvisionmanager.domain;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

/**
 * Parsed notification URL structure.
 * Contains all components extracted from notification URL string.
 *
 * Example URL: ntfy://user:pass@domain:8080/topic?title=App&scheme=https
 */
@Data
@Builder
public class ParsedNotificationUrl {

    /**
     * URL scheme (ntfy, ntfys, webhook, discord, etc.)
     */
    private String scheme;

    /**
     * Optional username for authentication
     */
    private String username;

    /**
     * Optional password for authentication
     */
    private String password;

    /**
     * Host/domain name
     */
    private String host;

    /**
     * Port number (default: 80 for HTTP, 443 for HTTPS)
     */
    private int port;

    /**
     * URL path (e.g., /topic, /webhook/endpoint)
     */
    private String path;

    /**
     * Topic extracted from path (for NTFY)
     * Path "/my-topic" becomes topic "my-topic"
     */
    private String topic;

    /**
     * Query parameters as key-value map
     * Common params: title, priority, tags, scheme
     */
    private Map<String, String> queryParams;

    /**
     * Whether to use HTTPS (from ?scheme=https param)
     * Defaults to HTTP if not specified
     */
    private boolean useHttps;

    /**
     * Checks if authentication is configured
     */
    public boolean hasAuthentication() {
        return username != null && password != null;
    }

    /**
     * Builds full server URL (protocol + host + port)
     * Example: https://domain:8080
     */
    public String getServerUrl() {
        String protocol = useHttps ? "https" : "http";

        // Default ports don't need to be included
        boolean isDefaultPort = (useHttps && port == 443) || (!useHttps && port == 80);

        if (isDefaultPort) {
            return String.format("%s://%s", protocol, host);
        } else {
            return String.format("%s://%s:%d", protocol, host, port);
        }
    }

    /**
     * Gets query parameter value or returns default
     */
    public String getQueryParam(String key, String defaultValue) {
        return queryParams.getOrDefault(key, defaultValue);
    }
}