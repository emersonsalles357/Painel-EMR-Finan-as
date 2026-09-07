package br.com.emr.emrfinancas.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
        @NotBlank @Size(max = 100) String senhaAtual,
        @NotBlank @Size(max = 72) String novaSenha) {}
