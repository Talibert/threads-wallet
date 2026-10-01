package com.example.api_docker.application.user.command;

public record CreateUserCommand(String firstName, String lastName, String email, String rawPassword) {}
