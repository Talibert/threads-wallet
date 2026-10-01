package com.example.api_docker.domain.shared;

import java.time.LocalDateTime;
import java.util.UUID;

public interface DomainEvent {
    UUID eventId();
    LocalDateTime occurredAt();
    EventType eventType();
}
