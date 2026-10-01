package com.example.api_docker.domain.user;

import java.util.Objects;
import java.util.UUID;

public record UserId(UUID value) {
    public UserId {
        Objects.requireNonNull(value, "UserId não pode ser nulo");
    }

    public static UserId generate() { return new UserId(UUID.randomUUID()); }
    public static UserId of(String raw) { return new UserId(UUID.fromString(raw)); }

    @Override public String toString() { return value.toString(); }
}
