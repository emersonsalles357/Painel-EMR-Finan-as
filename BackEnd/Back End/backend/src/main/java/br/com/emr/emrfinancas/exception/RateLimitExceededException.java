package br.com.emr.emrfinancas.exception;

public class RateLimitExceededException extends RuntimeException {
    public RateLimitExceededException(String message) {
        super(message);
    }

    public RateLimitExceededException() {
        super("Muitas tentativas. Tente novamente mais tarde.");
    }
}
