package com.example.api_docker.infra.controller.user.request;

import com.example.api_docker.application.user.command.ChangeUserPasswordCommand;
import com.example.api_docker.domain.user.UserId;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
        @Schema(description = "Senha atual do usuário", example = "senhaAntiga123")
        @NotBlank(message = "A senha atual é obrigatória")
        String currentPassword,

        @Schema(description = "Nova senha do usuário (mínimo 6 caracteres)", example = "novaSenha456")
        @NotBlank(message = "A nova senha é obrigatória")
        @Size(min = 6, message = "A nova senha deve ter no mínimo 6 caracteres")
        String newPassword
) {
    public ChangeUserPasswordCommand toCommand(UserId userId) {
        return new ChangeUserPasswordCommand(userId, currentPassword, newPassword);
    }
}
