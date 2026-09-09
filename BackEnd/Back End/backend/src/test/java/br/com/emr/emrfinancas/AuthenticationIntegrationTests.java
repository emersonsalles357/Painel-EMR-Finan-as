package br.com.emr.emrfinancas;

import br.com.emr.emrfinancas.model.Usuario;
import br.com.emr.emrfinancas.repository.UsuarioRepository;
import br.com.emr.emrfinancas.security.JwtService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthenticationIntegrationTests {
    private static final String EMAIL = "usuario@teste.com";
    private static final String SENHA = "SenhaSegura123!";

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired UsuarioRepository usuarioRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired JwtService jwtService;

    private Usuario usuario;

    @BeforeEach
    void prepararUsuario() {
        usuarioRepository.deleteAll();
        usuario = new Usuario();
        usuario.setNome("Usuario Teste");
        usuario.setEmail(EMAIL);
        usuario.setSenha(passwordEncoder.encode(SENHA));
        usuario = usuarioRepository.saveAndFlush(usuario);
    }

    @Test
    void loginCorretoRetornaTokenSemSenha() throws Exception {
        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(EMAIL, SENHA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(3600))
                .andExpect(jsonPath("$.usuario.email").value(EMAIL))
                .andReturn().getResponse().getContentAsString();

        assertThat(response).doesNotContain("senha", usuario.getSenha(), SENHA);
    }

    @Test
    void senhaIncorretaRetorna401Generico() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson(EMAIL, "senha-incorreta")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Credenciais invalidas."));
    }

    @Test
    void usuarioInexistenteRetornaMesmo401Generico() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson("inexistente@teste.com", SENHA)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Credenciais invalidas."));
    }

    @Test
    void endpointProtegidoSemTokenRetorna401() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void endpointProtegidoComTokenInvalidoRetorna401() throws Exception {
        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer token-invalido"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void endpointProtegidoComTokenAdulteradoRetorna401() throws Exception {
        String token = tokenValido();
        int signatureStart = token.lastIndexOf('.') + 1;
        char replacement = token.charAt(signatureStart) == 'a' ? 'b' : 'a';
        String adulterado = token.substring(0, signatureStart)
                + replacement + token.substring(signatureStart + 1);

        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + adulterado))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void endpointProtegidoComTokenExpiradoRetorna401() throws Exception {
        String expirado = jwtService.generateToken(userDetails(), Instant.now().minusSeconds(7200), 3600000);

        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + expirado))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenDeUsuarioRemovidoRetorna401() throws Exception {
        String token = tokenValido();
        usuarioRepository.delete(usuario);
        usuarioRepository.flush();

        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void endpointProtegidoComTokenValidoPermiteAcessoESerializaSomenteDadosPublicos() throws Exception {
        String response = mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + tokenValido()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(usuario.getCodigo()))
                .andExpect(jsonPath("$.email").value(EMAIL))
                .andExpect(jsonPath("$.role").value("ROLE_USER"))
                .andReturn().getResponse().getContentAsString();

        assertThat(response).doesNotContain("senha", usuario.getSenha(), SENHA);
    }

    @Test
    void usuarioComumNaoPodeCriarOutroUsuario() throws Exception {
        mockMvc.perform(post("/api/usuarios")
                        .header("Authorization", "Bearer " + tokenValido())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void bcryptGeraHashEValidaSenhaOriginal() {
        String hash = passwordEncoder.encode(SENHA);
        assertThat(hash).isNotEqualTo(SENHA).startsWith("$2");
        assertThat(passwordEncoder.matches(SENHA, hash)).isTrue();
    }

    private String tokenValido() {
        return jwtService.generateToken(userDetails());
    }

    private UserDetails userDetails() {
        return User.withUsername(EMAIL).password(usuario.getSenha()).authorities("ROLE_USER").build();
    }

    private String loginJson(String email, String senha) throws Exception {
        JsonNode node = objectMapper.createObjectNode().put("email", email).put("senha", senha);
        return objectMapper.writeValueAsString(node);
    }
}
