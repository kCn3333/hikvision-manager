package com.kcn.hikvisionmanager.service.notification.ntfy;

import com.kcn.hikvisionmanager.domain.NotificationRequest;
import com.kcn.hikvisionmanager.domain.ParsedNotificationUrl;
import com.kcn.hikvisionmanager.service.notification.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.Base64;
import java.util.List;

/**
 * NTFY notification service implementation.
 * Sends notifications via NTFY API using JSON format.
 *
 * NTFY API Documentation: <a href="https://docs.ntfy.sh/publish/">...</a>
 *
 * Supports:
 * - HTTP Basic Authentication
 * - Custom priorities (default, high, urgent)
 * - Emoji tags for visual categorization
 * - Markdown formatting in messages
 */
@Slf4j
public class NtfyNotificationService implements NotificationService {

    private final RestTemplate restTemplate;
    private final ParsedNotificationUrl config;
    private final String serverUrl;

    public NtfyNotificationService(RestTemplate restTemplate, ParsedNotificationUrl config) {
        this.restTemplate = restTemplate;
        this.config = config;
        this.serverUrl = config.getServerUrl();

        log.debug("NtfyNotificationService initialized for topic: {} at {}",
                config.getTopic(), serverUrl);
    }

    @Override
    public void send(String title, String message) {
        try {
            // Build notification request
            NotificationRequest request = buildNotificationRequest(title, message);

            // Prepare HTTP headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // Add authentication if configured
            if (config.hasAuthentication()) {
                String auth = config.getUsername() + ":" + config.getPassword();
                String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());
                headers.set("Authorization", "Basic " + encodedAuth);
            }

            HttpEntity<NotificationRequest> entity = new HttpEntity<>(request, headers);

            // Send POST request to NTFY server
            log.debug("Sending NTFY notification to topic: {}", config.getTopic());
            restTemplate.postForObject(serverUrl, entity, String.class);

            log.info("✅ NTFY notification sent successfully to topic: {}", config.getTopic());

        } catch (Exception e) {
            log.error("❌ Failed to send NTFY notification: {}", e.getMessage());
            throw new RuntimeException("NTFY notification delivery failed", e);
        }
    }

    @Override
    public String getType() {
        return "ntfy";
    }

    /**
     * Builds NTFY notification request with tags and formatting.
     */
    private NotificationRequest buildNotificationRequest(String title, String message) {

        // Get default title from config if not overridden
        String finalTitle = config.getQueryParam("title", title);

        return NotificationRequest.builder()
                .topic(config.getTopic())
                .title(finalTitle)
                .message(message)
                .tags(getTags())
                .build();
    }

    /**
     * Gets emoji tags based on notification priority.
     * Tags add visual indicators in NTFY mobile/desktop apps.
     */
    private List<String> getTags() {
        return Arrays.asList("backup", "white_check_mark");
    }
}