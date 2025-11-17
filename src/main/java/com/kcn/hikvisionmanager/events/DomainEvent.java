package com.kcn.hikvisionmanager.events;


import java.time.LocalDateTime;

public interface DomainEvent {
    LocalDateTime getOccurredAt();
}