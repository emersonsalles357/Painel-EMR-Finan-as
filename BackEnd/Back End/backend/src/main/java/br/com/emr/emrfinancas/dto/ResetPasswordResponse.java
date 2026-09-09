package br.com.emr.emrfinancas.dto;

public record ResetPasswordResponse(String message) {
    public static ResetPasswordResponse success() {
        return new ResetPasswordResponse("Senha redefinida com sucesso.");
    }
}
