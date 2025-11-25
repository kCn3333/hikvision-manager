package com.kcn.hikvisionmanager.service.notification.discord;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Discord webhook request payload.
 * Supports text content, embeds, and custom bot appearance.
 *
 * Discord Webhook API: https://discord.com/developers/docs/resources/webhook#execute-webhook
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DiscordWebhookRequest {

    /**
     * Simple text content (alternative to embeds)
     */
    private String content;

    /**
     * Override default webhook username
     */
    private String username;

    /**
     * Override default webhook avatar
     */
    @JsonProperty("avatar_url")
    private String avatarUrl;

    /**
     * List of rich embeds (max 10 per message)
     */
    private List<DiscordEmbed> embeds;

    /**
     * Text-to-speech enabled
     */
    private Boolean tts;
}