package com.example.api_docker.domain.user.event;

import com.example.api_docker.domain.shared.DomainEvent;
import com.example.api_docker.domain.shared.EventType;
import com.example.api_docker.domain.user.Email;
import com.example.api_docker.domain.user.UserId;

import java.time.LocalDateTime;
import java.util.UUID;

public record UserCreatedEvent(UUID eventId, LocalDateTime occurredAt,
                               UserId userId, Email email, EventType eventType) implements DomainEvent {

    public UserCreatedEvent(UserId userId, Email email){
        this(UUID.randomUUID(), LocalDateTime.now(), userId, email, EventType.USER_CREATED_EVENT);
    }
}
