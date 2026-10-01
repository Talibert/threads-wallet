package com.example.api_docker.infra.controller.user.response;

import com.example.api_docker.application.user.result.UserResult;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

public record UserResponse(
        @Schema(description = "Identificador único do usuário", example = "9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d")
        UUID userId,

        @Schema(description = "Nome completo do usuário", example = "Maria Silva")
        String fullName,

        @Schema(description = "E-mail cadastrado", example = "maria.silva@exemplo.com")
        String email,

        @Schema(description = "Data e hora de cadastro")
        LocalDateTime createdAt
) {
    public static UserResponse from(UserResult result) {
        return new UserResponse(
                result.userId(),
                result.fullName(),
                result.email(),
                result.createdAt()
        );
    }
}
