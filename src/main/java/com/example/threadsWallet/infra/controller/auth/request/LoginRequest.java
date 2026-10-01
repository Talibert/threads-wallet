package com.example.api_docker.infra.controller.auth.request;

import com.example.api_docker.application.auth.command.LoginCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @Schema(description = "E-mail cadastrado do usuário", example = "admin@course.com")
        @NotBlank(message = "Email não pode ser vazio")
        @Email(message = "Email inválido")
        String email,

        @Schema(description = "Senha do usuário", example = "MaluZoe")
        @NotBlank(message = "Senha não pode ser vazia")
        String password
) {
    public LoginCommand toCommand() {
        return new LoginCommand(email, password);
    }
}
