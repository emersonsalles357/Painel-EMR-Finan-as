package br.com.emr.emrfinancas.dto;

import jakarta.validation.constraints.NotBlank;

public record ResetPasswordRequest(
        @NotBlank(message = "O token e obrigatorio")
        String token,

        @NotBlank(message = "A nova senha e obrigatoria")
        String novaSenha
) {}
