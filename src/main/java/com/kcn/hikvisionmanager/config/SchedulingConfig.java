package com.kcn.hikvisionmanager.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Enables Spring's scheduled task execution capability.
 * Required for @Scheduled annotated methods across the application.
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}