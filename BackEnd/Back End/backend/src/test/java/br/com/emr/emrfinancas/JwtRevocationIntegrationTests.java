package br.com.emr.emrfinancas;

import br.com.emr.emrfinancas.dto.ResetPasswordRequest;
import br.com.emr.emrfinancas.model.PasswordResetToken;
import br.com.emr.emrfinancas.model.Usuario;
import br.com.emr.emrfinancas.repository.PasswordResetTokenRepository;
import br.com.emr.emrfinancas.repository.UsuarioRepository;
import br.com.emr.emrfinancas.service.PasswordResetService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class JwtRevocationIntegrationTests {
    private static final String OLD_PASSWORD = "SenhaAnterior@123";
    private static final String NEW_PASSWORD = "SenhaNova@456789";

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired UsuarioRepository usuarioRepository;
    @Autowired PasswordResetTokenRepository resetTokenRepository;
    @Autowired PasswordEncoder passwordEncoder;

    @BeforeEach
    void cleanDatabase() {
        resetTokenRepository.deleteAll();
        usuarioRepository.deleteAll();
    }

    @Test
    void authenticatedPasswordChangeRevokesOldTokenAndNewLoginTokenWorks() throws Exception {
        Usuario userA = createUser("Usuario A");
        Usuario userB = createUser("Usuario B");
        String oldTokenA = login(userA, OLD_PASSWORD);
        String tokenB = login(userB, OLD_PASSWORD);

        accessProfile(oldTokenA, 200);
        mockMvc.perform(post("/api/users/me/password")
                        .header("Authorization", bearer(oldTokenA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "senhaAtual", OLD_PASSWORD,
                                "novaSenha", NEW_PASSWORD))))
                .andExpect(status().isNoContent());

        accessProfile(oldTokenA, 401);
        accessProfile(tokenB, 200);
        assertThat(usuarioRepository.findById(userA.getCodigo()).orElseThrow().getTokenVersion()).isEqualTo(1);

        String newTokenA = login(userA, NEW_PASSWORD);
        assertThat(newTokenA).isNotEqualTo(oldTokenA);
        accessProfile(newTokenA, 200);
    }

    @Test
    void passwordResetRevokesOldTokenAndTokenIssuedAfterResetRemainsValid() throws Exception {
        Usuario user = createUser("Usuario Reset");
        String oldToken = login(user, OLD_PASSWORD);
        accessProfile(oldToken, 200);

        String rawResetToken = "reset-" + UUID.randomUUID();
        Instant now = Instant.now();
        resetTokenRepository.saveAndFlush(new PasswordResetToken(
                user,
                PasswordResetService.hashToken(rawResetToken),
                now.minusSeconds(1),
                now.plusSeconds(300)));

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ResetPasswordRequest(rawResetToken, NEW_PASSWORD))))
                .andExpect(status().isOk());

        accessProfile(oldToken, 401);
        assertThat(usuarioRepository.findById(user.getCodigo()).orElseThrow().getTokenVersion()).isEqualTo(1);

        String newToken = login(user, NEW_PASSWORD);
        accessProfile(newToken, 200);
    }

    @Test
    void changingOneUsersPasswordDoesNotAffectAnotherUsersTokens() throws Exception {
        Usuario userA = createUser("Usuario Isolado A");
        Usuario userB = createUser("Usuario Isolado B");
        String tokenA = login(userA, OLD_PASSWORD);
        String tokenB = login(userB, OLD_PASSWORD);

        mockMvc.perform(post("/api/users/me/password")
                        .header("Authorization", bearer(tokenA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "senhaAtual", OLD_PASSWORD,
                                "novaSenha", NEW_PASSWORD))))
                .andExpect(status().isNoContent());

        accessProfile(tokenA, 401);
        accessProfile(tokenB, 200);
        assertThat(usuarioRepository.findById(userB.getCodigo()).orElseThrow().getTokenVersion()).isZero();
    }

    private Usuario createUser(String name) {
        Usuario user = new Usuario();
        user.setNome(name);
        user.setEmail(UUID.randomUUID() + "@jwt.test");
        user.setSenha(passwordEncoder.encode(OLD_PASSWORD));
        return usuarioRepository.saveAndFlush(user);
    }

    private String login(Usuario user, String password) throws Exception {
        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", user.getEmail(),
                                "senha", password))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("accessToken").asText();
    }

    private void accessProfile(String token, int expectedStatus) throws Exception {
        mockMvc.perform(get("/api/auth/me").header("Authorization", bearer(token)))
                .andExpect(status().is(expectedStatus));
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
