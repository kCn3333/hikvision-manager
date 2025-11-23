package com.kcn.hikvisionmanager.util;

import com.kcn.hikvisionmanager.domain.ParsedNotificationUrl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Parser for notification URLs in Shoutrrr/Watchtower format.
 *
 * Supported format:
 * scheme://[username:password@]host[:port]/path[?param=value&...]
 *
 * Examples:
 * - ntfy://user:pass@domain/topic?title=MyApp&scheme=https
 * - ntfy://domain/my-topic?scheme=https
 * - webhook://domain:8080/notify?scheme=https
 */
@Component
@Slf4j
public class NotificationUrlParser {

    /**
     * Parses notification URL string into structured components.
     *
     * @param url Notification URL string
     * @return Parsed URL structure
     * @throws IllegalArgumentException if URL format is invalid
     */
    public ParsedNotificationUrl parse(String url) {
        try {
            URI uri = new URI(url);

            String scheme = uri.getScheme();
            if (scheme == null) {
                throw new IllegalArgumentException("Missing URL scheme (e.g., ntfy://)");
            }

            String host = uri.getHost();
            if (host == null) {
                throw new IllegalArgumentException("Missing host in URL");
            }

            // Parse authentication (username:password)
            String userInfo = uri.getUserInfo();
            String username = null;
            String password = null;
            if (userInfo != null) {
                String[] parts = userInfo.split(":", 2);
                username = URLDecoder.decode(parts[0], StandardCharsets.UTF_8);
                if (parts.length > 1) {
                    password = URLDecoder.decode(parts[1], StandardCharsets.UTF_8);
                }
            }

            // Parse port (default to -1 if not specified, will be set later)
            int port = uri.getPort();

            // Parse path and extract topic
            String path = uri.getPath();
            if (path == null || path.isEmpty() || path.equals("/")) {
                throw new IllegalArgumentException("Missing path/topic in URL");
            }

            // Remove leading slash for topic
            String topic = path.startsWith("/") ? path.substring(1) : path;

            // Parse query parameters
            Map<String, String> queryParams = parseQueryString(uri.getQuery());

            // Determine if HTTPS should be used (from ?scheme=https param)
            boolean useHttps = "https".equalsIgnoreCase(queryParams.get("scheme"));

            // Set default port if not specified
            if (port == -1) {
                port = useHttps ? 443 : 80;
            }

            log.info("Parsed notification URL - scheme: {}, host: {}, port: {}, topic: {}, https: {}",
                    scheme, host, port, topic, useHttps);

            return ParsedNotificationUrl.builder()
                    .scheme(scheme)
                    .username(username)
                    .password(password)
                    .host(host)
                    .port(port)
                    .path(path)
                    .topic(topic)
                    .queryParams(queryParams)
                    .useHttps(useHttps)
                    .build();

        } catch (Exception e) {
            log.error("Failed to parse notification URL: {}", e.getMessage());
            throw new IllegalArgumentException("Invalid notification URL format: " + e.getMessage(), e);
        }
    }

    /**
     * Parses URL query string into key-value map.
     *
     * @param query Query string (e.g., "title=MyApp&priority=high")
     * @return Map of query parameters
     */
    private Map<String, String> parseQueryString(String query) {
        Map<String, String> params = new HashMap<>();

        if (query == null || query.isEmpty()) {
            return params;
        }

        String[] pairs = query.split("&");
        for (String pair : pairs) {
            String[] keyValue = pair.split("=", 2);
            if (keyValue.length == 2) {
                String key = URLDecoder.decode(keyValue[0], StandardCharsets.UTF_8);
                String value = URLDecoder.decode(keyValue[1], StandardCharsets.UTF_8);
                params.put(key, value);
            } else if (keyValue.length == 1) {
                // Parameter without value (e.g., ?flag)
                String key = URLDecoder.decode(keyValue[0], StandardCharsets.UTF_8);
                params.put(key, "");
            }
        }

        return params;
    }
}