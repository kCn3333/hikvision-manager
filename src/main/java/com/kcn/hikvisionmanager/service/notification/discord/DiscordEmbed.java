package com.kcn.hikvisionmanager.service.notification.discord;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Discord embed structure for rich notification messages.
 * Supports colored embeds with fields, footer, and timestamp.
 *
 * Discord Webhook API: https://discord.com/developers/docs/resources/webhook
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DiscordEmbed {

    /**
     * Embed title
     */
    private String title;

    /**
     * Embed description (main message body)
     */
    private String description;

    /**
     * Color of the embed bar (decimal format)
     * Examples: 5763719 (green), 16776960 (yellow), 15548997 (red)
     */
    private Integer color;

    /**
     * List of embed fields (displayed in grid)
     */
    private List<EmbedField> fields;

    /**
     * Footer information
     */
    private EmbedFooter footer;

    /**
     * ISO 8601 timestamp
     */
    private String timestamp;

    /**
     * Thumbnail image
     */
    private EmbedThumbnail thumbnail;

    /**
     * Embed field structure
     */
    @Data
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class EmbedField {
        /**
         * Field name/label
         */
        private String name;

        /**
         * Field value/content
         */
        private String value;

        /**
         * Whether field should be displayed inline
         */
        private Boolean inline;
    }

    /**
     * Embed footer structure
     */
    @Data
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class EmbedFooter {
        /**
         * Footer text
         */
        private String text;

        /**
         * Footer icon URL
         */
        @JsonProperty("icon_url")
        private String iconUrl;
    }

    /**
     * Embed thumbnail structure
     */
    @Data
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class EmbedThumbnail {
        /**
         * Thumbnail image URL
         */
        private String url;
    }
}