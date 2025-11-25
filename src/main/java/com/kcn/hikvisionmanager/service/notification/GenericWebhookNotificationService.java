package com.kcn.hikvisionmanager.service.notification;

import com.kcn.hikvisionmanager.domain.ParsedNotificationUrl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Generic webhook notification service implementation.
 * Sends JSON POST requests to any HTTP endpoint with configurable headers.
 *
 * URL Format: webhook://domain/endpoint?scheme=https&header_Authorization=Bearer token&header_X-Custom=value
 *
 * Features:
 * - Custom HTTP headers via query params (prefix: header_)
 * - JSON payload with title, message, priority, timestamp
 * - HTTP/HTTPS support
 * - Works with Zapier, Make.com, n8n, custom APIs
 *
 * Examples:
 * - webhook://domain/api/notify?scheme=https
 * - webhook://domain/webhook?scheme=https&header_Authorization=Bearer abc123
 * - webhook://domain/hook?scheme=https&header_X-API-Key=secret&header_X-Source=hikvision
 */
@Slf4j
public class GenericWebhookNotificationService implements NotificationService {

    private final RestTemplate restTemplate;
    private final ParsedNotificationUrl config;
    private final String webhookUrl;
    private final HttpHeaders headers;

    public GenericWebhookNotificationService(RestTemplate restTemplate, ParsedNotificationUrl config) {
        this.restTemplate = restTemplate;
        this.config = config;

        // Build full webhook URL
        this.webhookUrl = config.getServerUrl() + config.getPath();

        // Build HTTP headers from query params
        this.headers = buildHeaders();

        log.debug("GenericWebhookNotificationService initialized for: {}", webhookUrl);
    }

    @Override
    public void send(String title, String message) {
        try {
            // Build JSON payload
            Map<String, Object> payload = buildPayload(title, message);

            // Set Content-Type if not already set
            if (!headers.containsKey(HttpHeaders.CONTENT_TYPE)) {
                headers.setContentType(MediaType.APPLICATION_JSON);
            }

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);

            // Send POST request to webhook
            log.debug("Sending generic webhook notification to: {}", webhookUrl);
            restTemplate.postForObject(webhookUrl, entity, String.class);

            log.info("✅ Generic webhook notification sent successfully to: {}", webhookUrl);

        } catch (Exception e) {
            log.error("❌ Failed to send generic webhook notification: {}", e.getMessage());
            throw new RuntimeException("Generic webhook notification delivery failed", e);
        }
    }

    @Override
    public String getType() {
        return "webhook";
    }

    /**
     * Builds JSON payload for webhook POST request.
     */
    private Map<String, Object> buildPayload(String title, String message) {
        Map<String, Object> payload = new HashMap<>();

        payload.put("title", title);
        payload.put("message", message);
        payload.put("timestamp", Instant.now().toString());
        payload.put("source", "hikvision-manager");

        // Add metadata from query params if available
        Map<String, String> metadata = new HashMap<>();
        config.getQueryParams().forEach((key, value) -> {
            // Skip internal params and headers
            if (!key.startsWith("header_") && !key.equals("scheme")) {
                metadata.put(key, value);
            }
        });

        if (!metadata.isEmpty()) {
            payload.put("metadata", metadata);
        }

        return payload;
    }

    /**
     * Builds HTTP headers from query parameters.
     * Query params starting with "header_" are converted to HTTP headers.
     *
     * Example: ?header_Authorization=Bearer token → Authorization: Bearer token
     */
    private HttpHeaders buildHeaders() {
        HttpHeaders httpHeaders = new HttpHeaders();

        config.getQueryParams().forEach((key, value) -> {
            if (key.startsWith("header_")) {
                // Remove "header_" prefix to get actual header name
                String headerName = key.substring(7); // "header_".length() = 7
                httpHeaders.set(headerName, value);
                log.debug("Added custom header: {} = ***", headerName);
            }
        });

        return httpHeaders;
    }
}