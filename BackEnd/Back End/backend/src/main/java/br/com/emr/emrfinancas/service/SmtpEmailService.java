package br.com.emr.emrfinancas.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class SmtpEmailService implements EmailService {
    private static final Logger LOGGER = LoggerFactory.getLogger(SmtpEmailService.class);

    private final JavaMailSender mailSender;
    private final String fromEmail;

    public SmtpEmailService(JavaMailSender mailSender,
                            @Value("${app.mail.from:noreply@emrfinancas.com.br}") String fromEmail) {
        this.mailSender = mailSender;
        this.fromEmail = fromEmail;
    }

    @Override
    public void sendPasswordResetEmail(String toEmail, String resetLink) {
        try {
            LOGGER.info("Iniciando envio de e-mail de recuperacao de senha para destinatario seguro");
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("EMR Finanças - Recuperação de Senha");
            message.setText("Olá,\n\n"
                    + "Você solicitou a recuperação de senha da sua conta no EMR Finanças.\n"
                    + "Para redefinir sua senha, acesse o link abaixo:\n\n"
                    + resetLink + "\n\n"
                    + "Este link expira em 15 minutos e só pode ser utilizado uma vez.\n"
                    + "Se você não solicitou a alteração, ignore este e-mail.\n\n"
                    + "Atenciosamente,\n"
                    + "Equipe EMR Finanças");

            mailSender.send(message);
            LOGGER.info("E-mail de recuperacao de senha enviado com sucesso");
        } catch (Exception e) {
            // Mail exceptions can contain the complete message, including its reset token.
            LOGGER.error("Falha na entrega SMTP de recuperacao de senha ({})", e.getClass().getSimpleName());
        }
    }
}
