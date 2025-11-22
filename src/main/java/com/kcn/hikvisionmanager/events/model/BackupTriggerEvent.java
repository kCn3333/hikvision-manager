package com.kcn.hikvisionmanager.events.model;

import com.kcn.hikvisionmanager.events.DomainEvent;

import java.time.LocalDateTime;

public record BackupTriggerEvent(String configId) implements DomainEvent {
    @Override
    public LocalDateTime getOccurredAt() {
        return LocalDateTime.now();
    }
}