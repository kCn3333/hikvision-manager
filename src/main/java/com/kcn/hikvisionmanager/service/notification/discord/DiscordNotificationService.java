package com.kcn.hikvisionmanager.service.notification.discord;

import com.kcn.hikvisionmanager.domain.ParsedNotificationUrl;
import com.kcn.hikvisionmanager.service.notification.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Discord webhook notification service implementation.
 * Sends rich embedded messages to Discord channels via webhooks.
 *
 * Discord Webhook Documentation: https://discord.com/developers/docs/resources/webhook
 *
 * URL Format: discord://webhook-id/webhook-token?username=BotName&avatar_url=https://...
 *
 * Features:
 * - Rich embeds with colors
 * - Inline fields for structured data
 * - Custom bot username and avatar
 * - Timestamp support
 */
@Slf4j
public class DiscordNotificationService implements NotificationService {

    private final RestTemplate restTemplate;
    private final ParsedNotificationUrl config;
    private final String webhookUrl;
    private final String username;
    private final String avatarUrl;

    public DiscordNotificationService(RestTemplate restTemplate, ParsedNotificationUrl config) {
        this.restTemplate = restTemplate;
        this.config = config;

        // Discord webhook URL format: https://discord.com/api/webhooks/{webhook-id}/{webhook-token}
        String webhookId = config.getHost();
        String webhookToken = config.getTopic(); // Path becomes token

        this.webhookUrl = String.format("https://discord.com/api/webhooks/%s/%s",
                webhookId, webhookToken);

        // Get optional customization from query params
        this.username = config.getQueryParam("username", "Hikvision Manager");
        this.avatarUrl = config.getQueryParam("avatar_url", null);

        log.debug("DiscordNotificationService initialized for webhook: {}", maskWebhookUrl());
    }

    @Override
    public void send(String title, String message) {
        try {
            // Parse message to extract structured data
            Map<String, String> fields = parseMessageFields(message);

            // Build Discord embed
            DiscordEmbed embed = buildEmbed(title, message, fields);

            // Build webhook request
            DiscordWebhookRequest request = DiscordWebhookRequest.builder()
                    .username(username)
                    .avatarUrl(avatarUrl)
                    .embeds(List.of(embed))
                    .build();

            // Prepare HTTP headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<DiscordWebhookRequest> entity = new HttpEntity<>(request, headers);

            // Send POST request to Discord webhook
            log.debug("Sending Discord notification to webhook");
            restTemplate.postForObject(webhookUrl, entity, String.class);

            log.info("✅ Discord notification sent successfully");

        } catch (Exception e) {
            log.error("❌ Failed to send Discord notification: {}", e.getMessage());
            throw new RuntimeException("Discord notification delivery failed", e);
        }
    }

    @Override
    public String getType() {
        return "discord";
    }

    /**
     * Builds Discord embed with title, description, color, and fields.
     */
    private DiscordEmbed buildEmbed(String title, String message, Map<String, String> fields) {

        List<DiscordEmbed.EmbedField> embedFields = new ArrayList<>();

        // Add structured fields if available
        if (fields.containsKey("recordings")) {
            embedFields.add(DiscordEmbed.EmbedField.builder()
                    .name("📊 Recordings")
                    .value(fields.get("recordings"))
                    .inline(true)
                    .build());
        }

        if (fields.containsKey("size") && !fields.get("size").equals("0 B")) {
            embedFields.add(DiscordEmbed.EmbedField.builder()
                    .name("💾 Size")
                    .value(fields.get("size"))
                    .inline(true)
                    .build());
        }

        if (fields.containsKey("duration") && fields.get("duration") != null) {
            embedFields.add(DiscordEmbed.EmbedField.builder()
                    .name("⏱️ Duration")
                    .value(fields.get("duration"))
                    .inline(true)
                    .build());
        }

        if (fields.containsKey("directory")) {
            embedFields.add(DiscordEmbed.EmbedField.builder()
                    .name("📂 Location")
                    .value("`" + fields.get("directory") + "`")
                    .inline(false)
                    .build());
        }

        return DiscordEmbed.builder()
                .title(title)
                .description(getEmbedDescription(message, fields))
                .fields(embedFields.isEmpty() ? null : embedFields)
                .footer(DiscordEmbed.EmbedFooter.builder()
                        .text("Hikvision Manager")
                        .build())
                .timestamp(Instant.now().toString())
                .build();
    }

    /**
     * Extracts description from message, removing structured data.
     */
    private String getEmbedDescription(String message, Map<String, String> fields) {
        // If message contains structured data, remove it for cleaner embed
        if (!fields.isEmpty()) {
            // Return only the first line (status header)
            String[] lines = message.split("\n");
            if (lines.length > 0) {
                return lines[0].replace("**", ""); // Remove markdown bold
            }
        }
        return message;
    }

    /**
     * Parses message to extract structured field data.
     * Looks for patterns like "📊 **Recordings:** 45/50"
     */
    private Map<String, String> parseMessageFields(String message) {
        Map<String, String> fields = new java.util.HashMap<>();

        String[] lines = message.split("\n");
        for (String line : lines) {
            // Parse "📊 **Recordings:** 45/50 completed"
            if (line.contains("Recordings:")) {
                String value = line.substring(line.indexOf("Recordings:") + 11).trim();
                fields.put("recordings", value);
            }
            // Parse "💾 **Size:** 2.3 GB"
            else if (line.contains("Size:")) {
                String value = line.substring(line.indexOf("Size:") + 5).trim();
                fields.put("size", value);
            }
            // Parse "⏱️ **Duration:** 12 minutes"
            else if (line.contains("Duration:")) {
                String value = line.substring(line.indexOf("Duration:") + 9).trim();
                fields.put("duration", value);
            }
            // Parse "📂 **Location:** /path"
            else if (line.contains("Location:")) {
                String value = line.substring(line.indexOf("Location:") + 9).trim();
                fields.put("directory", value.replace("`", ""));
            }
        }

        return fields;
    }

    /**
     * Masks webhook URL for safe logging (hides token).
     */
    private String maskWebhookUrl() {
        return webhookUrl.replaceAll("/[^/]+$", "/***");
    }
}