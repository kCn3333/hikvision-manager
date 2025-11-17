package com.kcn.hikvisionmanager.events.model;

import com.kcn.hikvisionmanager.domain.BatchDownloadJob;
import com.kcn.hikvisionmanager.events.DomainEvent;

import java.time.LocalDateTime;

public record BackupDownloadStartedEvent(
        String backupBatchId,
        LocalDateTime occurredAt
) implements DomainEvent {

    @Override
    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }
}

