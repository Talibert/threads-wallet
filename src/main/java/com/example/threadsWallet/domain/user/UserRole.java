package com.example.api_docker.domain.user;

public enum UserRole {
    USER,
    ADMIN;

    public String toSecurityRole() {
        return "ROLE_" + this.name();
    }
}
