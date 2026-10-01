package com.example.api_docker.application.user.result;

import com.example.api_docker.domain.user.User;

import java.time.LocalDateTime;
import java.util.UUID;

public record UserResult(
        UUID userId,
        String fullName,
        String email,
        LocalDateTime createdAt
) {
    public static UserResult from(User user) {
        return new UserResult(
                user.getId().value(),
                user.getName().full(),
                user.getEmail().value(),
                user.getCreatedAt()
        );
    }
}
