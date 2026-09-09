package br.com.emr.emrfinancas;

import br.com.emr.emrfinancas.model.UserRole;
import br.com.emr.emrfinancas.model.Usuario;
import br.com.emr.emrfinancas.repository.GastoRepository;
import br.com.emr.emrfinancas.repository.UsuarioRepository;
import com.fasterxml.jackson.databind.JsonNode;
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

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class RegistrationIntegrationTests {
    private static final String PASSWORD = "Senha@12345";

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired UsuarioRepository usuarioRepository;
    @Autowired GastoRepository gastoRepository;
    @Autowired PasswordEncoder passwordEncoder;

    @BeforeEach
    void limparDados() {
        gastoRepository.deleteAll();
        usuarioRepository.deleteAll();
    }

    @Test
    void cadastroValidoNormalizaDadosPersisteBcryptEUserSemExporSenha() throws Exception {
        String response = register("  Maria da Silva  ", "  Usuario@Email.COM  ", PASSWORD)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nome").value("Maria da Silva"))
                .andExpect(jsonPath("$.email").value("usuario@email.com"))
                .andExpect(jsonPath("$.mensagem").value("Conta criada com sucesso"))
                .andExpect(jsonPath("$.senha").doesNotExist())
                .andExpect(jsonPath("$.role").doesNotExist())
                .andReturn().getResponse().getContentAsString();

        Usuario persisted = usuarioRepository.findByEmail("usuario@email.com").orElseThrow();
        assertThat(persisted.getNome()).isEqualTo("Maria da Silva");
        assertThat(persisted.getRole()).isEqualTo(UserRole.USER);
        assertThat(persisted.getSenha()).isNotEqualTo(PASSWORD).startsWith("$2");
        assertThat(passwordEncoder.matches(PASSWORD, persisted.getSenha())).isTrue();
        assertThat(response).doesNotContain(PASSWORD, persisted.getSenha(), "senha");
    }

    @Test
    void emailDuplicadoIgnoraCasingERetorna409() throws Exception {
        register("Primeiro Usuario", "Usuario@Email.com", PASSWORD).andExpect(status().isCreated());
        register("Segundo Usuario", "usuario@email.com", PASSWORD)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("E-mail ja cadastrado"));
    }

    @Test
    void rejeitaNomeVazioOuSomenteEspacos() throws Exception {
        register("", "um@email.com", PASSWORD).andExpect(status().isBadRequest());
        register("   ", "dois@email.com", PASSWORD).andExpect(status().isBadRequest());
    }

    @Test
    void rejeitaEmailVazioOuInvalido() throws Exception {
        register("Usuario Um", "", PASSWORD).andExpect(status().isBadRequest());
        register("Usuario Dois", "email-invalido", PASSWORD).andExpect(status().isBadRequest());
    }

    @Test
    void rejeitaCadaViolacaoDaPoliticaDeSenha() throws Exception {
        assertWeakPassword("Aa1@curta", "pelo menos 10 caracteres");
        assertWeakPassword("senhafraca1@", "letra maiuscula");
        assertWeakPassword("SENHAFRACA1@", "letra minuscula");
        assertWeakPassword("SenhaSemNumero@", "um numero");
        assertWeakPassword("SenhaSemEspecial1", "caractere especial");
    }

    @Test
    void roleForjadaNuncaCriaAdministrador() throws Exception {
        Map<String, Object> payload = Map.of(
                "nome", "Atacante", "email", "atacante@email.com", "senha", PASSWORD, "role", "ADMIN");
        register(payload).andExpect(status().isCreated());

        usuarioRepository.findByEmail("atacante@email.com")
                .ifPresent(usuario -> assertThat(usuario.getRole()).isEqualTo(UserRole.USER));
    }

    @Test
    void roleAdminPersistidaEUsadaSomenteNaAutorizacaoDeContaProvisionada() throws Exception {
        Usuario admin = new Usuario();
        admin.setNome("Administrador Controlado");
        admin.setEmail("admin@interno.com");
        admin.setSenha(passwordEncoder.encode(PASSWORD));
        admin.setRole(UserRole.ADMIN);
        usuarioRepository.saveAndFlush(admin);

        String token = login("ADMIN@INTERNO.COM", PASSWORD);
        mockMvc.perform(get("/api/usuarios").header("Authorization", bearer(token)))
                .andExpect(status().isOk());
    }

    @Test
    void usuarioRecemCadastradoFazLoginUsandoCasingDiferenteEMantemAutorizacaoUser() throws Exception {
        register("Novo Usuario", "novo@email.com", PASSWORD).andExpect(status().isCreated());
        String token = login("NOVO@EMAIL.COM", PASSWORD);

        mockMvc.perform(get("/api/auth/me").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("novo@email.com"))
                .andExpect(jsonPath("$.role").value("ROLE_USER"));
        mockMvc.perform(get("/api/usuarios").header("Authorization", bearer(token)))
                .andExpect(status().isForbidden());
    }

    @Test
    void usuariosRegistradosContinuamIsoladosFinanceiramente() throws Exception {
        register("Usuario A", "a@registro.com", PASSWORD).andExpect(status().isCreated());
        register("Usuario B", "b@registro.com", PASSWORD).andExpect(status().isCreated());
        String tokenA = login("a@registro.com", PASSWORD);
        String tokenB = login("b@registro.com", PASSWORD);

        String gasto = objectMapper.writeValueAsString(Map.of(
                "descricao", "Dado privado A", "categoria", "Teste", "valor", 100,
                "data", "2026-09-04"));
        String created = mockMvc.perform(post("/api/gastos").header("Authorization", bearer(tokenA))
                        .contentType(MediaType.APPLICATION_JSON).content(gasto))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long id = objectMapper.readTree(created).get("codigo").asLong();

        mockMvc.perform(get("/api/gastos/{id}", id).header("Authorization", bearer(tokenB)))
                .andExpect(status().isNotFound());
        String listB = mockMvc.perform(get("/api/gastos").header("Authorization", bearer(tokenB)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(listB).doesNotContain("Dado privado A");
    }

    private org.springframework.test.web.servlet.ResultActions register(String nome, String email, String senha)
            throws Exception {
        return register(Map.of("nome", nome, "email", email, "senha", senha));
    }

    private org.springframework.test.web.servlet.ResultActions register(Map<String, Object> payload) throws Exception {
        return mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)));
    }

    private void assertWeakPassword(String password, String expectedMessage) throws Exception {
        String email = "teste" + Math.abs(password.hashCode()) + "@email.com";
        register("Usuario Teste", email, password)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString(expectedMessage)));
    }

    private String login(String email, String senha) throws Exception {
        String response = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", email, "senha", senha))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        JsonNode json = objectMapper.readTree(response);
        return json.get("accessToken").asText();
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
