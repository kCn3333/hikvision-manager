package com.kcn.hikvisionmanager.domain;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * Universal notification request DTO.
 * Can be used for NTFY, webhooks, and other notification providers.
 *
 * This model is designed to be provider-agnostic - each provider
 * can map these fields to its specific API format.
 */
@Data
@Builder
public class NotificationRequest {

    /**
     * Notification topic/channel (used by NTFY)
     */
    private String topic;

    /**
     * Notification title/subject
     */
    private String title;

    /**
     * Notification message body (supports markdown for NTFY)
     */
    private String message;

//    /**
//     * Priority level: default, high, urgent
//     */
//    private String priority;

    /**
     * Tags for categorization and emoji icons
     * Example: ["backup", "white_check_mark", "camera"]
     */
    private List<String> tags;

    /**
     * Optional URL to open when notification is clicked
     */
    private String click;

    /**
     * Additional provider-specific fields
     * Can be used by custom webhook implementations
     */
    private Map<String, Object> extras;
}