package com.kcn.hikvisionmanager.config;

import com.kcn.hikvisionmanager.domain.ParsedNotificationUrl;
import com.kcn.hikvisionmanager.service.notification.NotificationService;
import com.kcn.hikvisionmanager.service.notification.NtfyNotificationService;
import com.kcn.hikvisionmanager.util.NotificationUrlParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Configuration for notification services.
 * Creates RestTemplate for notifications and initializes notification providers
 * based on configured URLs.
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class NotificationConfiguration {

    private final NotificationProperties notificationProperties;

    /**
     * Creates dedicated RestTemplate for notification delivery.
     * Configured with timeouts from notification properties.
     *
     * @param builder injected by Spring, pre-configured with sensible defaults
     * @return Configured RestTemplate instance
     */
    @Bean
    public RestTemplate notificationRestTemplate(RestTemplateBuilder builder) {
        return builder
                .connectTimeout(Duration.ofSeconds(notificationProperties.getTimeoutSeconds())) // was setConnectTimeout
                .readTimeout(Duration.ofSeconds(notificationProperties.getTimeoutSeconds()))    // was setReadTimeout
                .build();
    }

    /**
     * Creates list of notification service implementations based on configured URLs.
     * Supports multiple notification providers simultaneously.
     * Invalid URLs are logged and skipped without stopping application startup.
     *
     * @param notificationRestTemplate RestTemplate for HTTP requests
     * @param urlParser URL parser for notification URLs
     * @return List of active notification services
     */
    @Bean
    public List<NotificationService> notificationServices(
            RestTemplate notificationRestTemplate,
            NotificationUrlParser urlParser) {

        List<NotificationService> services = new ArrayList<>();

        // Filter out empty/blank URLs
        List<String> validUrls = notificationProperties.getUrls().stream()
                .filter(url -> url != null && !url.isBlank())
                .toList();

        if (validUrls.isEmpty()) {
            log.info("ℹ️ No notification URLs configured - notifications disabled");
            return services;
        }

        for (String url : validUrls) {
            try {
                ParsedNotificationUrl parsed = urlParser.parse(url);

                NotificationService service = switch (parsed.getScheme()) {
                    case "ntfy", "ntfys" -> new NtfyNotificationService(
                            notificationRestTemplate, parsed);
                    // Future providers:
                    // case "webhook" -> new WebhookNotificationService(...)
                    // case "discord" -> new DiscordNotificationService(...)
                    default -> {
                        log.warn("⚠️ Unknown notification scheme '{}' in URL: {}",
                                parsed.getScheme(), maskUrl(url));
                        yield null;
                    }
                };

                if (service != null) {
                    services.add(service);
                    log.info("✅ Registered {} notification service: {}",
                            service.getType(), maskUrl(url));
                }

            } catch (Exception e) {
                log.error("❌ Failed to parse notification URL: {} - {}",
                        maskUrl(url), e.getMessage());
                // Continue with other URLs - don't fail application startup
            }
        }

        if (services.isEmpty()) {
            log.warn("⚠️ No valid notification services initialized");
        } else {
            log.info("🔔 Initialized {} notification service(s)", services.size());
        }

        return services;
    }

    /**
     * Masks sensitive information (username/password) in URLs for logging.
     *
     * @param url Original URL
     * @return Masked URL with credentials hidden
     */
    private String maskUrl(String url) {
        if (url.contains("@")) {
            return url.replaceAll("://[^@]+@", "://***:***@");
        }
        return url;
    }
}