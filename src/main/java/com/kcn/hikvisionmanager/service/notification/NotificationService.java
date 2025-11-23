package com.kcn.hikvisionmanager.service.notification;


/**
 * Interface for notification delivery services.
 * Implementations provide integration with specific notification platforms
 * (NTFY, webhooks, Discord, Slack, etc.).
 */
public interface NotificationService {

    /**
     * Sends notification with specified title, message, and priority.
     *
     * @param title Notification title/subject
     * @param message Notification message body
     * @throws RuntimeException if notification delivery fails
     */
    void send(String title, String message);

    /**
     * Returns the type identifier of this notification service.
     *
     * @return Service type (e.g., "ntfy", "webhook", "discord")
     */
    String getType();
}