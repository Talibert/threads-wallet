package com.example.api_docker.domain.user;

public interface TokenGenerator {
    String generate(UserId userId, Email email, UserRole userRole);
}
