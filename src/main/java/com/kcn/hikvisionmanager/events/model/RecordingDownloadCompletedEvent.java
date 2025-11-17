package com.kcn.hikvisionmanager.events.model;

import com.kcn.hikvisionmanager.events.DomainEvent;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;

@Slf4j
public record RecordingDownloadCompletedEvent(
        String recordingId,
        String batchId,
        Long actualFileSizeBytes,
        LocalDateTime occurredAt
) implements DomainEvent {

    @Override
    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }
}