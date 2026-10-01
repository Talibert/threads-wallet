package com.example.api_docker.infra.controller.auth.response;

import com.example.api_docker.application.auth.result.LoginResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

public record LoginResponse(
        @Schema(description = "Token JWT de autenticação")
        String token,

        @Schema(description = "Identificador único do usuário", example = "9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d")
        UUID userId,

        @Schema(description = "Nome completo do usuário", example = "Admin Course")
        String fullName,

        @Schema(description = "Perfil de acesso", example = "ADMIN")
        String role
) {
    public static LoginResponse from(LoginResult result) {
        return new LoginResponse(
                result.token(),
                result.userId(),
                result.fullName(),
                result.role()
        );
    }
}
