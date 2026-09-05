package br.com.emr.emrfinancas;

import br.com.emr.emrfinancas.dto.ForgotPasswordRequest;
import br.com.emr.emrfinancas.dto.LoginRequest;
import br.com.emr.emrfinancas.dto.RegisterRequest;
import br.com.emr.emrfinancas.dto.ResetPasswordRequest;
import br.com.emr.emrfinancas.model.Usuario;
import br.com.emr.emrfinancas.repository.UsuarioRepository;
import br.com.emr.emrfinancas.security.RateLimiterService;
import br.com.emr.emrfinancas.service.EmailService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthenticationLockoutAndRateLimitTests {
    private static final String EMAIL = "lockout@teste.com";
    private static final String SENHA = "SenhaSegura123!";

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired UsuarioRepository usuarioRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired RateLimiterService rateLimiterService;

    @MockBean EmailService emailService;

    private Usuario usuario;

    @BeforeEach
    void setUp() {
        rateLimiterService.clear();
        usuarioRepository.deleteAll();

        usuario = new Usuario();
        usuario.setNome("Usuario Bloqueio");
        usuario.setEmail(EMAIL);
        usuario.setSenha(passwordEncoder.encode(SENHA));
        usuario = usuarioRepository.saveAndFlush(usuario);
    }

    @Test
    void primeiraESegundaFalhasIncrementamContadorSemBloquear() throws Exception {
        // 1ª falha
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(EMAIL, "errada-1"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Credenciais invalidas."));

        Usuario u1 = usuarioRepository.findById(usuario.getCodigo()).orElseThrow();
        assertThat(u1.getFailedLoginAttempts()).isEqualTo(1);
        assertThat(u1.getLockedUntil()).isNull();

        // 2ª falha
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(EMAIL, "errada-2"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Credenciais invalidas."));

        Usuario u2 = usuarioRepository.findById(usuario.getCodigo()).orElseThrow();
        assertThat(u2.getFailedLoginAttempts()).isEqualTo(2);
        assertThat(u2.getLockedUntil()).isNull();
    }

    @Test
    void terceiraFalhaBloqueiaContaPor15Minutos() throws Exception {
        for (int i = 1; i <= 3; i++) {
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new LoginRequest(EMAIL, "errada-" + i))))
                    .andExpect(status().isUnauthorized());
        }

        Usuario u3 = usuarioRepository.findById(usuario.getCodigo()).orElseThrow();
        assertThat(u3.getFailedLoginAttempts()).isEqualTo(3);
        assertThat(u3.getLockedUntil()).isNotNull();
        assertThat(u3.getLockedUntil()).isAfter(Instant.now());
    }

    @Test
    void tentativaCorretaDuranteBloqueioNaoAutentica() throws Exception {
        usuario.setFailedLoginAttempts(3);
        usuario.setLockedUntil(Instant.now().plus(15, ChronoUnit.MINUTES));
        usuarioRepository.saveAndFlush(usuario);

        // Mesmo com a senha correta, não deve permitir login
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(EMAIL, SENHA))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Credenciais invalidas."));
    }

    @Test
    void fimDoPeriodoDeBloqueioLiberaContaEAutentica() throws Exception {
        usuario.setFailedLoginAttempts(3);
        // Bloqueio já expirou (no passado)
        usuario.setLockedUntil(Instant.now().minus(1, ChronoUnit.MINUTES));
        usuarioRepository.saveAndFlush(usuario);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(EMAIL, SENHA))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty());

        Usuario liberado = usuarioRepository.findById(usuario.getCodigo()).orElseThrow();
        assertThat(liberado.getFailedLoginAttempts()).isEqualTo(0);
        assertThat(liberado.getLockedUntil()).isNull();
    }

    @Test
    void loginValidoZeraFalhasAnteriores() throws Exception {
        usuario.setFailedLoginAttempts(2);
        usuarioRepository.saveAndFlush(usuario);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(EMAIL, SENHA))))
                .andExpect(status().isOk());

        Usuario resetado = usuarioRepository.findById(usuario.getCodigo()).orElseThrow();
        assertThat(resetado.getFailedLoginAttempts()).isEqualTo(0);
    }

    @Test
    void usuarioInexistenteNaoCriaRegistroNemAlteraEstado() throws Exception {
        long contagemAntes = usuarioRepository.count();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("inexistente@teste.com", "QualquerSenha123!"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Credenciais invalidas."));

        assertThat(usuarioRepository.count()).isEqualTo(contagemAntes);
    }

    @Test
    void loginExcedendoRateLimitRetorna429() throws Exception {
        // Limite por IP + email é 5 tentativas
        for (int i = 1; i <= 5; i++) {
            mockMvc.perform(post("/api/auth/login")
                            .header("X-Forwarded-For", "198.51.100.1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new LoginRequest(EMAIL, "senha-" + i))))
                    .andExpect(status().isUnauthorized());
        }

        // 6ª tentativa dispara rate limit (429)
        mockMvc.perform(post("/api/auth/login")
                        .header("X-Forwarded-For", "198.51.100.1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(EMAIL, "senha-6"))))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.status").value(429))
                .andExpect(jsonPath("$.message").value("Muitas tentativas. Tente novamente mais tarde."));
    }

    @Test
    void forgotPasswordExcedendoRateLimitRetorna429() throws Exception {
        // Limite é 5 tentativas por IP
        for (int i = 1; i <= 5; i++) {
            mockMvc.perform(post("/api/auth/forgot-password")
                            .header("X-Forwarded-For", "198.51.100.2")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new ForgotPasswordRequest("teste" + i + "@teste.com"))))
                    .andExpect(status().isOk());
        }

        // 6ª tentativa dispara rate limit (429)
        mockMvc.perform(post("/api/auth/forgot-password")
                        .header("X-Forwarded-For", "198.51.100.2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ForgotPasswordRequest("teste6@teste.com"))))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.status").value(429));
    }

    @Test
    void registerExcedendoRateLimitRetorna429() throws Exception {
        // Limite é 5 tentativas por IP
        for (int i = 1; i <= 5; i++) {
            mockMvc.perform(post("/api/auth/register")
                            .header("X-Forwarded-For", "198.51.100.3")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new RegisterRequest("User " + i, "reg" + i + "@teste.com", "SenhaValida123!"))))
                    .andExpect(status().isCreated());
        }

        // 6ª tentativa dispara rate limit (429)
        mockMvc.perform(post("/api/auth/register")
                        .header("X-Forwarded-For", "198.51.100.3")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RegisterRequest("User 6", "reg6@teste.com", "SenhaValida123!"))))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.status").value(429));
    }

    @Test
    void resetPasswordExcedendoRateLimitRetorna429() throws Exception {
        // Limite é 10 tentativas por IP
        for (int i = 1; i <= 10; i++) {
            mockMvc.perform(post("/api/auth/reset-password")
                            .header("X-Forwarded-For", "198.51.100.4")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new ResetPasswordRequest("token-falso-" + i, "NovaSenhaSegura123!"))))
                    .andExpect(status().isBadRequest());
        }

        // 11ª tentativa dispara rate limit (429)
        mockMvc.perform(post("/api/auth/reset-password")
                        .header("X-Forwarded-For", "198.51.100.4")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ResetPasswordRequest("token-falso-11", "NovaSenhaSegura123!"))))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.status").value(429));
    }
}
