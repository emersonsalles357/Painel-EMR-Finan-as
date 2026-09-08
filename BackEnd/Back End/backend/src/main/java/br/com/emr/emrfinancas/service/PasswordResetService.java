package br.com.emr.emrfinancas.service;

import br.com.emr.emrfinancas.dto.ForgotPasswordRequest;
import br.com.emr.emrfinancas.dto.ForgotPasswordResponse;
import br.com.emr.emrfinancas.dto.ResetPasswordRequest;
import br.com.emr.emrfinancas.dto.ResetPasswordResponse;
import br.com.emr.emrfinancas.exception.RegraNegocioException;
import br.com.emr.emrfinancas.model.PasswordResetToken;
import br.com.emr.emrfinancas.model.Usuario;
import br.com.emr.emrfinancas.repository.PasswordResetTokenRepository;
import br.com.emr.emrfinancas.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;

@Service
public class PasswordResetService {
    private static final Logger LOGGER = LoggerFactory.getLogger(PasswordResetService.class);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UsuarioRepository usuarioRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicyValidator passwordPolicyValidator;
    private final EmailNormalizer emailNormalizer;
    private final Clock clock;
    private final int expirationMinutes;
    private final String frontendUrl;

    public PasswordResetService(
            UsuarioRepository usuarioRepository,
            PasswordResetTokenRepository passwordResetTokenRepository,
            EmailService emailService,
            PasswordEncoder passwordEncoder,
            PasswordPolicyValidator passwordPolicyValidator,
            EmailNormalizer emailNormalizer,
            Clock clock,
            @Value("${app.security.password-reset.expiration-minutes:15}") int expirationMinutes,
            @Value("${app.frontend.url:http://localhost:5173}") String frontendUrl) {
        this.usuarioRepository = usuarioRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.emailService = emailService;
        this.passwordEncoder = passwordEncoder;
        this.passwordPolicyValidator = passwordPolicyValidator;
        this.emailNormalizer = emailNormalizer;
        this.clock = clock;
        this.expirationMinutes = expirationMinutes;
        this.frontendUrl = frontendUrl;
    }

    @Transactional
    public ForgotPasswordResponse solicitarRecuperacao(ForgotPasswordRequest request) {
        String normalizedEmail = emailNormalizer.normalize(request.email());
        Optional<Usuario> usuarioOpt = usuarioRepository.findByEmailIgnoreCase(normalizedEmail);

        if (usuarioOpt.isPresent()) {
            Usuario usuario = usuarioOpt.get();
            Instant now = clock.instant();

            // Invalida tokens anteriores ativos do usuario
            passwordResetTokenRepository.invalidateActiveTokensForUser(usuario, now);

            // Gera token bruto criptograficamente aleatório (32 bytes em Base64 URL-safe)
            byte[] randomBytes = new byte[32];
            SECURE_RANDOM.nextBytes(randomBytes);
            String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);

            // Armazena unicamente o hash SHA-256 no banco
            String tokenHash = hashToken(rawToken);
            Instant expiresAt = now.plus(expirationMinutes, ChronoUnit.MINUTES);

            PasswordResetToken resetToken = new PasswordResetToken(usuario, tokenHash, now, expiresAt);
            passwordResetTokenRepository.save(resetToken);

            // Monta o link para o front-end e envia o e-mail
            String resetLink = buildResetLink(rawToken);
            emailService.sendPasswordResetEmail(usuario.getEmail(), resetLink);

            LOGGER.info("Solicitacao de recuperacao de senha processada com sucesso");
        } else {
            LOGGER.info("Solicitacao de recuperacao recebida para e-mail nao cadastrado");
        }

        // Anti-enumeração: sempre retorna a mesma resposta genérica
        return ForgotPasswordResponse.defaultMessage();
    }

    @Transactional
    public ResetPasswordResponse redefinirSenha(ResetPasswordRequest request) {
        String tokenHash = hashToken(request.token().trim());
        Instant now = clock.instant();

        PasswordResetToken tokenEntity = passwordResetTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new RegraNegocioException("Token de recuperacao invalido, expirado ou ja utilizado."));

        if (!tokenEntity.isValid(now)) {
            throw new RegraNegocioException("Token de recuperacao invalido, expirado ou ja utilizado.");
        }

        // Valida política de senha
        passwordPolicyValidator.validate(request.novaSenha());

        // Atualiza senha com BCrypt
        Usuario usuario = tokenEntity.getUsuario();
        usuario.setSenha(passwordEncoder.encode(request.novaSenha()));
        usuario.resetLockout();
        usuario.invalidateTokens();
        usuarioRepository.save(usuario);

        // Invalida o token marcando como utilizado
        tokenEntity.setUsedAt(now);
        passwordResetTokenRepository.save(tokenEntity);

        LOGGER.info("Senha redefinida com sucesso para o usuario");
        return ResetPasswordResponse.success();
    }

    public static String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Algoritmo SHA-256 indisponivel", e);
        }
    }

    private String buildResetLink(String rawToken) {
        String base = frontendUrl.endsWith("/") ? frontendUrl.substring(0, frontendUrl.length() - 1) : frontendUrl;
        return base + "/redefinir-senha?token=" + rawToken;
    }
}
