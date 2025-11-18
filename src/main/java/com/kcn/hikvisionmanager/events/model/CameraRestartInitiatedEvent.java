package com.kcn.hikvisionmanager.events.model;

import com.kcn.hikvisionmanager.events.DomainEvent;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * Event published when camera restart is initiated.
 * Signals that camera will be offline for the grace period duration.
 */
@Getter
public class CameraRestartInitiatedEvent implements DomainEvent {

    private final int gracePeriodSeconds;
    private final LocalDateTime occurredAt;

    public CameraRestartInitiatedEvent(int gracePeriodSeconds, LocalDateTime occurredAt) {
        this.gracePeriodSeconds = gracePeriodSeconds;
        this.occurredAt = occurredAt;
    }
}