package br.com.emr.emrfinancas.dto;

public record ForgotPasswordResponse(String message) {
    public static final String DEFAULT_MESSAGE =
            "Se existir uma conta associada a este e-mail, enviaremos as instruções de recuperação.";

    public static ForgotPasswordResponse defaultMessage() {
        return new ForgotPasswordResponse(DEFAULT_MESSAGE);
    }
}
