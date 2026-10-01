package com.example.api_docker.infra.controller.user.request;

import com.example.api_docker.application.user.command.CreateUserCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(
        @Schema(description = "Primeiro nome do usuário", example = "Maria")
        @NotBlank(message = "Nome não pode ser vazio")
        String firstName,

        @Schema(description = "Sobrenome do usuário", example = "Silva")
        @NotBlank(message = "Sobrenome não pode ser vazio")
        String lastName,

        @Schema(description = "E-mail único do usuário", example = "maria.silva@exemplo.com")
        @NotBlank(message = "Email não pode ser vazio")
        @Email(message = "Email inválido")
        String email,

        @Schema(description = "Senha de acesso (mínimo 8 caracteres)", example = "senhaSegura123")
        @NotBlank(message = "Senha não pode ser vazia")
        @Size(min = 8, message = "Senha deve ter no mínimo 8 caracteres")
        String password
) {
    public CreateUserCommand toCommand() {
        return new CreateUserCommand(firstName, lastName, email, password);
    }
}
