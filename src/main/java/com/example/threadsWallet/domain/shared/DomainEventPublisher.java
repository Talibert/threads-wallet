package com.example.api_docker.domain.shared;

public interface DomainEventPublisher {
    void publish(DomainEvent event);
}
