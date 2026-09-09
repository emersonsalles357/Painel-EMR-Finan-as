package br.com.emr.emrfinancas.exception;

public class EmailJaCadastradoException extends RuntimeException {
    public EmailJaCadastradoException() {
        super("E-mail ja cadastrado");
    }
}
