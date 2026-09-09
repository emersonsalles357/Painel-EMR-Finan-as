package br.com.emr.emrfinancas;

import br.com.emr.emrfinancas.model.Gasto;
import br.com.emr.emrfinancas.model.Investimento;
import br.com.emr.emrfinancas.model.Recebimento;
import br.com.emr.emrfinancas.model.Usuario;
import br.com.emr.emrfinancas.repository.GastoRepository;
import br.com.emr.emrfinancas.repository.InvestimentoRepository;
import br.com.emr.emrfinancas.repository.RecebimentoRepository;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class MultiUserDataIsolationIntegrationTests {
    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired UsuarioRepository usuarioRepository;
    @Autowired GastoRepository gastoRepository;
    @Autowired RecebimentoRepository recebimentoRepository;
    @Autowired InvestimentoRepository investimentoRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired JwtService jwtService;

    private Usuario usuarioA;
    private Usuario usuarioB;
    private String tokenA;
    private String tokenB;

    @BeforeEach
    void prepararUsuarios() {
        investimentoRepository.deleteAll();
        recebimentoRepository.deleteAll();
        gastoRepository.deleteAll();
        usuarioRepository.deleteAll();

        usuarioA = salvarUsuario("Usuario A", "a@teste.com");
        usuarioB = salvarUsuario("Usuario B", "b@teste.com");
        tokenA = token(usuarioA);
        tokenB = token(usuarioB);
    }

    @Test
    void gastosFicamIsoladosEProtegidosContraIdorETrocaDeProprietario() throws Exception {
        long gastoA = criar("/api/gastos", tokenA, gastoJson("Gasto A", usuarioB));
        long gastoB = criar("/api/gastos", tokenB, gastoJson("Gasto B", usuarioA));

        assertThat(gastoRepository.findById(gastoA).orElseThrow().getUsuario().getCodigo())
                .isEqualTo(usuarioA.getCodigo());
        assertThat(gastoRepository.findById(gastoB).orElseThrow().getUsuario().getCodigo())
                .isEqualTo(usuarioB.getCodigo());
        verificarListagemIsolada("/api/gastos", tokenA, "Gasto A", "Gasto B");
        verificarListagemIsolada("/api/gastos", tokenB, "Gasto B", "Gasto A");
        verificarIdor("/api/gastos", gastoB, tokenA, gastoJson("Ataque", usuarioA));

        mockMvc.perform(put("/api/gastos/{id}", gastoA).header("Authorization", bearer(tokenA))
                        .contentType(MediaType.APPLICATION_JSON).content(gastoJson("Gasto A editado", usuarioB)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.usuario").doesNotExist());
        assertThat(gastoRepository.findById(gastoA).orElseThrow().getUsuario().getCodigo())
                .isEqualTo(usuarioA.getCodigo());
    }

    @Test
    void recebimentosFicamIsoladosEProtegidosContraIdorETrocaDeProprietario() throws Exception {
        long recebimentoA = criar("/api/recebimentos", tokenA, recebimentoJson("Recebimento A", usuarioB));
        long recebimentoB = criar("/api/recebimentos", tokenB, recebimentoJson("Recebimento B", usuarioA));

        assertThat(recebimentoRepository.findById(recebimentoA).orElseThrow().getUsuario().getCodigo())
                .isEqualTo(usuarioA.getCodigo());
        assertThat(recebimentoRepository.findById(recebimentoB).orElseThrow().getUsuario().getCodigo())
                .isEqualTo(usuarioB.getCodigo());
        verificarListagemIsolada("/api/recebimentos", tokenA, "Recebimento A", "Recebimento B");
        verificarListagemIsolada("/api/recebimentos", tokenB, "Recebimento B", "Recebimento A");
        verificarIdor("/api/recebimentos", recebimentoB, tokenA, recebimentoJson("Ataque", usuarioA));

        mockMvc.perform(put("/api/recebimentos/{id}", recebimentoA).header("Authorization", bearer(tokenA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(recebimentoJson("Recebimento A editado", usuarioB)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.usuario").doesNotExist());
        assertThat(recebimentoRepository.findById(recebimentoA).orElseThrow().getUsuario().getCodigo())
                .isEqualTo(usuarioA.getCodigo());
    }

    @Test
    void investimentosFicamIsoladosEProtegidosContraIdorETrocaDeProprietario() throws Exception {
        long investimentoA = criar("/api/investimentos", tokenA, investimentoJson("Investimento A", usuarioB));
        long investimentoB = criar("/api/investimentos", tokenB, investimentoJson("Investimento B", usuarioA));

        assertThat(investimentoRepository.findById(investimentoA).orElseThrow().getUsuario().getCodigo())
                .isEqualTo(usuarioA.getCodigo());
        assertThat(investimentoRepository.findById(investimentoB).orElseThrow().getUsuario().getCodigo())
                .isEqualTo(usuarioB.getCodigo());
        verificarListagemIsolada("/api/investimentos", tokenA, "Investimento A", "Investimento B");
        verificarListagemIsolada("/api/investimentos", tokenB, "Investimento B", "Investimento A");
        verificarIdor("/api/investimentos", investimentoB, tokenA, investimentoJson("Ataque", usuarioA));

        mockMvc.perform(put("/api/investimentos/{id}", investimentoA).header("Authorization", bearer(tokenA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(investimentoJson("Investimento A editado", usuarioB)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.usuario").doesNotExist());
        assertThat(investimentoRepository.findById(investimentoA).orElseThrow().getUsuario().getCodigo())
                .isEqualTo(usuarioA.getCodigo());
    }

    @Test
    void dashboardSomaExclusivamenteDadosDoUsuarioAutenticado() throws Exception {
        salvarDadosDashboard(usuarioA, "10000.00", "2000.00", "3000.00");
        salvarDadosDashboard(usuarioB, "500.00", "450.00", "20.00");

        mockMvc.perform(get("/api/dashboard").header("Authorization", bearer(tokenA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRecebimentos").value(10000.0))
                .andExpect(jsonPath("$.totalGastos").value(2000.0))
                .andExpect(jsonPath("$.totalInvestimentos").value(3000.0))
                .andExpect(jsonPath("$.saldo").value(11000.0));

        mockMvc.perform(get("/api/dashboard").header("Authorization", bearer(tokenB)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRecebimentos").value(500.0))
                .andExpect(jsonPath("$.totalGastos").value(450.0))
                .andExpect(jsonPath("$.totalInvestimentos").value(20.0))
                .andExpect(jsonPath("$.saldo").value(70.0));
    }

    @Test
    void usuarioComumNaoPodeListarNemConsultarUsuarios() throws Exception {
        mockMvc.perform(get("/api/usuarios").header("Authorization", bearer(tokenA)))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.status").value(403));
        mockMvc.perform(get("/api/usuarios/{id}", usuarioB.getCodigo()).header("Authorization", bearer(tokenA)))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void administradorMantemLeituraAdministrativaSemExporSenha() throws Exception {
        String response = mockMvc.perform(get("/api/usuarios"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].email").exists())
                .andReturn().getResponse().getContentAsString();
        assertThat(response).doesNotContain("senha", usuarioA.getSenha(), usuarioB.getSenha());
    }

    private Usuario salvarUsuario(String nome, String email) {
        Usuario usuario = new Usuario();
        usuario.setNome(nome);
        usuario.setEmail(email);
        usuario.setSenha(passwordEncoder.encode("SenhaSegura123!"));
        return usuarioRepository.saveAndFlush(usuario);
    }

    private String token(Usuario usuario) {
        return jwtService.generateToken(User.withUsername(usuario.getEmail()).password(usuario.getSenha())
                .authorities("ROLE_USER").build());
    }

    private long criar(String endpoint, String token, String json) throws Exception {
        String response = mockMvc.perform(post(endpoint).header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.usuario").doesNotExist())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("codigo").asLong();
    }

    private void verificarListagemIsolada(String endpoint, String token, String presente, String ausente)
            throws Exception {
        String response = mockMvc.perform(get(endpoint).header("Authorization", bearer(token)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(response).contains(presente).doesNotContain(ausente, "usuario", "senha");
    }

    private void verificarIdor(String endpoint, long idAlheio, String tokenAtacante, String payload)
            throws Exception {
        mockMvc.perform(get(endpoint + "/{id}", idAlheio).header("Authorization", bearer(tokenAtacante)))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.message").value("Recurso nao encontrado"));
        mockMvc.perform(put(endpoint + "/{id}", idAlheio).header("Authorization", bearer(tokenAtacante))
                        .contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.message").value("Recurso nao encontrado"));
        mockMvc.perform(delete(endpoint + "/{id}", idAlheio).header("Authorization", bearer(tokenAtacante)))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.message").value("Recurso nao encontrado"));
    }

    private String gastoJson(String descricao, Usuario proprietarioForjado) throws Exception {
        JsonNode json = objectMapper.createObjectNode()
                .put("descricao", descricao).put("categoria", "Teste").put("valor", 100)
                .put("data", "2026-09-01").put("usuarioId", proprietarioForjado.getCodigo())
                .set("usuario", objectMapper.createObjectNode().put("codigo", proprietarioForjado.getCodigo()));
        return objectMapper.writeValueAsString(json);
    }

    private String recebimentoJson(String descricao, Usuario proprietarioForjado) throws Exception {
        JsonNode json = objectMapper.createObjectNode()
                .put("descricao", descricao).put("origem", "Teste").put("valor", 100)
                .put("data", "2026-09-01").put("status", "Recebido")
                .put("usuarioId", proprietarioForjado.getCodigo())
                .set("usuario", objectMapper.createObjectNode().put("codigo", proprietarioForjado.getCodigo()));
        return objectMapper.writeValueAsString(json);
    }

    private String investimentoJson(String nome, Usuario proprietarioForjado) throws Exception {
        JsonNode json = objectMapper.createObjectNode()
                .put("nome", nome).put("tipo", "Renda Fixa").put("instituicao", "Teste")
                .put("valorAplicado", 100).put("rentabilidadeMensal", 1).put("dataAplicacao", "2026-09-01")
                .put("usuarioId", proprietarioForjado.getCodigo())
                .set("usuario", objectMapper.createObjectNode().put("codigo", proprietarioForjado.getCodigo()));
        return objectMapper.writeValueAsString(json);
    }

    private void salvarDadosDashboard(Usuario usuario, String recebimentoValor, String gastoValor,
                                      String investimentoValor) {
        Recebimento recebimento = new Recebimento();
        recebimento.setDescricao("Receita " + usuario.getNome());
        recebimento.setOrigem("Teste");
        recebimento.setValor(new BigDecimal(recebimentoValor));
        recebimento.setData(LocalDate.of(2026, 9, 1));
        recebimento.setStatus("Recebido");
        recebimento.setUsuario(usuario);
        recebimentoRepository.save(recebimento);

        Gasto gasto = new Gasto();
        gasto.setDescricao("Gasto " + usuario.getNome());
        gasto.setCategoria("Teste");
        gasto.setValor(new BigDecimal(gastoValor));
        gasto.setData(LocalDate.of(2026, 9, 1));
        gasto.setUsuario(usuario);
        gastoRepository.save(gasto);

        Investimento investimento = new Investimento();
        investimento.setNome("Investimento " + usuario.getNome());
        investimento.setTipo("Renda Fixa");
        investimento.setValorAplicado(new BigDecimal(investimentoValor));
        investimento.setDataAplicacao(LocalDate.of(2026, 9, 1));
        investimento.setUsuario(usuario);
        investimentoRepository.save(investimento);
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
