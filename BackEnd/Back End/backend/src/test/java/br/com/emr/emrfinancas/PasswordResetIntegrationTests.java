package br.com.emr.emrfinancas;

import br.com.emr.emrfinancas.dto.ForgotPasswordRequest;
import br.com.emr.emrfinancas.dto.ForgotPasswordResponse;
import br.com.emr.emrfinancas.dto.LoginRequest;
import br.com.emr.emrfinancas.dto.ResetPasswordRequest;
import br.com.emr.emrfinancas.model.PasswordResetToken;
import br.com.emr.emrfinancas.model.Usuario;
import br.com.emr.emrfinancas.repository.PasswordResetTokenRepository;
import br.com.emr.emrfinancas.repository.UsuarioRepository;
import br.com.emr.emrfinancas.security.RateLimiterService;
import br.com.emr.emrfinancas.service.EmailService;
import br.com.emr.emrfinancas.service.PasswordResetService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class PasswordResetIntegrationTests {
    private static final String EMAIL = "recuperacao@teste.com";
    private static final String SENHA_ORIGINAL = "SenhaOriginal123!";
    private static final String NOVA_SENHA = "NovaSenhaSegura456@";

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired UsuarioRepository usuarioRepository;
    @Autowired PasswordResetTokenRepository tokenRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired RateLimiterService rateLimiterService;

    @MockBean EmailService emailService;

    private Usuario usuario;

    @BeforeEach
    void setUp() {
        rateLimiterService.clear();
        tokenRepository.deleteAll();
        usuarioRepository.deleteAll();

        usuario = new Usuario();
        usuario.setNome("Usuario Recuperacao");
        usuario.setEmail(EMAIL);
        usuario.setSenha(passwordEncoder.encode(SENHA_ORIGINAL));
        usuario = usuarioRepository.saveAndFlush(usuario);
    }

    @Test
    void emailExistenteGeraTokenEEnviaEmailComMensagemGenerica() throws Exception {
        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ForgotPasswordRequest(EMAIL))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(ForgotPasswordResponse.DEFAULT_MESSAGE));

        ArgumentCaptor<String> linkCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailService).sendPasswordResetEmail(eq(EMAIL), linkCaptor.capture());

        String link = linkCaptor.getValue();
        assertThat(link).contains("/redefinir-senha?token=");
        String rawToken = link.substring(link.indexOf("token=") + 6);
        assertThat(rawToken).hasSizeGreaterThanOrEqualTo(32);

        List<PasswordResetToken> tokens = tokenRepository.findAll();
        assertThat(tokens).hasSize(1);
        PasswordResetToken tokenEntity = tokens.get(0);
        // O banco armazena o hash SHA-256 e não o token bruto
        assertThat(tokenEntity.getTokenHash()).hasSize(64);
        assertThat(tokenEntity.getTokenHash()).isNotEqualTo(rawToken);
        assertThat(tokenEntity.getTokenHash()).isEqualTo(PasswordResetService.hashToken(rawToken));
    }

    @Test
    void emailInexistenteRetornaRespostaIdenticaSemEnviarEmail() throws Exception {
        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ForgotPasswordRequest("desconhecido@teste.com"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(ForgotPasswordResponse.DEFAULT_MESSAGE));

        verify(emailService, never()).sendPasswordResetEmail(anyString(), anyString());
        assertThat(tokenRepository.findAll()).isEmpty();
    }

    @Test
    void respostasPublicasDeEmailExistenteEInexistenteSaoIndistinguiveis() throws Exception {
        String respExistente = mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ForgotPasswordRequest(EMAIL))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String respInexistente = mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ForgotPasswordRequest("outro@teste.com"))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(respExistente).isEqualTo(respInexistente);
    }

    @Test
    void resetComTokenValidoAtualizaSenhaComBcryptPermiteNovoLoginEInvalidaSenhaAntiga() throws Exception {
        String rawToken = solicitarToken(EMAIL);

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ResetPasswordRequest(rawToken, NOVA_SENHA))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Senha redefinida com sucesso."));

        Usuario usuarioAtualizado = usuarioRepository.findById(usuario.getCodigo()).orElseThrow();
        assertThat(passwordEncoder.matches(NOVA_SENHA, usuarioAtualizado.getSenha())).isTrue();
        assertThat(passwordEncoder.matches(SENHA_ORIGINAL, usuarioAtualizado.getSenha())).isFalse();

        // Login com a senha antiga deve falhar (401)
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(EMAIL, SENHA_ORIGINAL))))
                .andExpect(status().isUnauthorized());

        // Login com a nova senha deve ter sucesso (200)
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(EMAIL, NOVA_SENHA))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty());
    }

    @Test
    void resetComTokenInvalidoFalha() throws Exception {
        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ResetPasswordRequest("token-inexistente-1234567890", NOVA_SENHA))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Token de recuperacao invalido, expirado ou ja utilizado."));
    }

    @Test
    void resetComTokenAdulteradoFalha() throws Exception {
        String rawToken = solicitarToken(EMAIL);
        String tokenAdulterado = rawToken.substring(0, rawToken.length() - 1) + (rawToken.endsWith("a") ? "b" : "a");

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ResetPasswordRequest(tokenAdulterado, NOVA_SENHA))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Token de recuperacao invalido, expirado ou ja utilizado."));
    }

    @Test
    void resetComTokenExpiradoFalha() throws Exception {
        String rawToken = solicitarToken(EMAIL);

        PasswordResetToken tokenEntity = tokenRepository.findAll().get(0);
        tokenEntity.setExpiresAt(Instant.now().minusSeconds(120));
        tokenRepository.saveAndFlush(tokenEntity);

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ResetPasswordRequest(rawToken, NOVA_SENHA))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Token de recuperacao invalido, expirado ou ja utilizado."));
    }

    @Test
    void tokenUtilizadoNaoPodeSerReutilizado() throws Exception {
        String rawToken = solicitarToken(EMAIL);

        // Primeiro uso com sucesso
        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ResetPasswordRequest(rawToken, NOVA_SENHA))))
                .andExpect(status().isOk());

        // Segundo uso deve falhar
        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ResetPasswordRequest(rawToken, "OutraSenhaSegura789#"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Token de recuperacao invalido, expirado ou ja utilizado."));
    }

    @Test
    void segundaSolicitacaoInvalidaTokenAnterior() throws Exception {
        String primeiroToken = solicitarToken(EMAIL);
        String segundoToken = solicitarToken(EMAIL);

        // O primeiro token deve ser rejeitado pois foi invalidado pela segunda solicitação
        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ResetPasswordRequest(primeiroToken, NOVA_SENHA))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Token de recuperacao invalido, expirado ou ja utilizado."));

        // O segundo token deve funcionar normalmente
        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ResetPasswordRequest(segundoToken, NOVA_SENHA))))
                .andExpect(status().isOk());
    }

    @Test
    void resetComSenhaFracaRetornaErroDeValidacao() throws Exception {
        String rawToken = solicitarToken(EMAIL);

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ResetPasswordRequest(rawToken, "fraca"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void respostasNaoExpoemDadosSensiveis() throws Exception {
        String rawToken = solicitarToken(EMAIL);

        String resetResponse = mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ResetPasswordRequest(rawToken, NOVA_SENHA))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(resetResponse).doesNotContain(rawToken, NOVA_SENHA, usuario.getSenha());
    }

    private String solicitarToken(String email) throws Exception {
        ArgumentCaptor<String> linkCaptor = ArgumentCaptor.forClass(String.class);
        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ForgotPasswordRequest(email))))
                .andExpect(status().isOk());

        verify(emailService, org.mockito.Mockito.atLeastOnce()).sendPasswordResetEmail(eq(email), linkCaptor.capture());
        List<String> links = linkCaptor.getAllValues();
        String link = links.get(links.size() - 1);
        return link.substring(link.indexOf("token=") + 6);
    }
}
