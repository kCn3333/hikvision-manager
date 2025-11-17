package com.kcn.hikvisionmanager.events.model;

import com.kcn.hikvisionmanager.entity.BackupConfigurationEntity;
import com.kcn.hikvisionmanager.events.DomainEvent;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;

@Slf4j
public record BackupTriggerEvent(
        BackupConfigurationEntity configuration
) implements DomainEvent {

    @Override
    public LocalDateTime getOccurredAt() {
        return LocalDateTime.now();
    }
}
