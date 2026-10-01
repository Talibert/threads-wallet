package com.example.api_docker.application.auth.command;

public record LoginCommand(String email, String rawPassword) {}
