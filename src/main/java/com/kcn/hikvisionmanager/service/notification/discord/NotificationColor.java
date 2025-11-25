package com.kcn.hikvisionmanager.service.notification.discord;

/**
 * Color constants for Discord embeds and other visual notifications.
 * Colors are in decimal format (converted from hex).
 */
public enum NotificationColor {
    /**
     * Green - successful operations
     * Hex: #58C27D
     */
    SUCCESS(5763719),

    /**
     * Yellow - warnings or partial failures
     * Hex: #FFFF00
     */
    WARNING(16776960),

    /**
     * Red - errors or complete failures
     * Hex: #ED4245
     */
    ERROR(15548997),

    /**
     * Blue - informational messages
     * Hex: #5865F2
     */
    INFO(5793522);

    private final int decimal;

    NotificationColor(int decimal) {
        this.decimal = decimal;
    }

    /**
     * Gets decimal color value for Discord embeds
     */
    public int getDecimal() {
        return decimal;
    }

    /**
     * Gets hex color string
     */
    public String getHex() {
        return String.format("#%06X", decimal);
    }
}