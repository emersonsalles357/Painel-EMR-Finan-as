package br.com.emr.emrfinancas.service;

public interface EmailService {
    void sendPasswordResetEmail(String toEmail, String resetLink);
}
