package com.example.api_docker.application.user.command;

import com.example.api_docker.domain.user.UserId;

public record ChangeUserPasswordCommand(
        UserId userId,
        String rawCurrentPassword,
        String rawNewPassword
) {}
