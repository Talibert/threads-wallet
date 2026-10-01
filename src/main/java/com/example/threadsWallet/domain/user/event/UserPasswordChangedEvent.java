package com.example.api_docker.domain.user.event;

import com.example.api_docker.domain.shared.DomainEvent;
import com.example.api_docker.domain.shared.EventType;
import com.example.api_docker.domain.user.UserId;

import java.time.LocalDateTime;
import java.util.UUID;

public record UserPasswordChangedEvent(UUID eventId, LocalDateTime occurredAt,
                                       UserId userId, EventType eventType) implements DomainEvent {

    public UserPasswordChangedEvent(UserId userId) {
        this(UUID.randomUUID(), LocalDateTime.now(), userId, EventType.USER_PASSWORD_CHANGED_EVENT);
    }
}
