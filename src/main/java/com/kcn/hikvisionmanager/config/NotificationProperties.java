package com.kcn.hikvisionmanager.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration properties for notification services.
 *
 * Example URLs:
 * - ntfy://username:password@domain/topic?title=MyApp&priority=default&scheme=https
 * - ntfy://domain/topic?scheme=https (without auth)
 * - webhook://domain/endpoint?scheme=https
 */
@Data
@Component
@ConfigurationProperties(prefix = "notification")
public class NotificationProperties {

    /**
     * List of notification URLs.
     * Format: scheme://[user:pass@]host[:port]/path[?params]
     *
     * Examples:
     * - "ntfy://user:pass@domain/hikvision-backups?title=Hikvision Backup&scheme=https"
     * - "ntfy://domain/my-topic?scheme=https"
     * - "webhook://domain/notify?scheme=https"
     */
    private List<String> urls = new ArrayList<>();

    /**
     * HTTP request timeout in seconds for notification delivery
     */
    private int timeoutSeconds = 10;
}