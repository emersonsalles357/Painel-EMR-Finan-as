package br.com.emr.emrfinancas;

import br.com.emr.emrfinancas.dto.GastoRequest;
import br.com.emr.emrfinancas.dto.InvestimentoRequest;
import br.com.emr.emrfinancas.dto.RecebimentoRequest;
import br.com.emr.emrfinancas.dto.RegisterRequest;
import br.com.emr.emrfinancas.model.Gasto;
import br.com.emr.emrfinancas.model.Usuario;
import br.com.emr.emrfinancas.repository.GastoRepository;
import br.com.emr.emrfinancas.repository.InvestimentoRepository;
import br.com.emr.emrfinancas.repository.RecebimentoRepository;
import br.com.emr.emrfinancas.repository.UsuarioRepository;
import br.com.emr.emrfinancas.service.AuthService;
import br.com.emr.emrfinancas.service.DashboardService;
import br.com.emr.emrfinancas.service.GastoService;
import br.com.emr.emrfinancas.service.InvestimentoService;
import br.com.emr.emrfinancas.service.RecebimentoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("postgres-it")
@EnabledIfEnvironmentVariable(named = "EMR_LOCAL_PG_URL", matches = ".+")
class PostgresDatabaseLiveIntegrationTests {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private GastoRepository gastoRepository;

    @Autowired
    private RecebimentoRepository recebimentoRepository;

    @Autowired
    private InvestimentoRepository investimentoRepository;

    @Autowired
    private AuthService authService;

    @Autowired
    private GastoService gastoService;

    @Autowired
    private RecebimentoService recebimentoService;

    @Autowired
    private InvestimentoService investimentoService;

    @Autowired
    private DashboardService dashboardService;

    @BeforeEach
    void limparDados() {
        jdbcTemplate.execute("TRUNCATE tb_emr_password_reset_token, tb_emr_investimento, tb_emr_recebimento, tb_emr_gasto, tb_emr_usuario RESTART IDENTITY CASCADE");
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Valida conexao com PostgreSQL 16 e metadados do driver")
    void validaMetadadosPostgres16() throws Exception {
        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData meta = conn.getMetaData();
            assertThat(meta.getDatabaseProductName()).isEqualTo("PostgreSQL");
            assertThat(meta.getDatabaseMajorVersion()).isEqualTo(16);
            String versionSetting = jdbcTemplate.queryForObject("SELECT current_setting('server_version')", String.class);
            assertThat(versionSetting).isNotNull().startsWith("16.");
        }
    }

    @Test
    @DisplayName("Valida que todas as 6 migrations Flyway foram aplicadas com sucesso no PostgreSQL")
    void validaFlywayMigrations() {
        Integer migrationCount = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM flyway_schema_history WHERE success = true", Integer.class);
        assertThat(migrationCount).isNotNull().isGreaterThanOrEqualTo(6);

        List<String> versions = jdbcTemplate.queryForList(
                "SELECT version FROM flyway_schema_history ORDER BY installed_rank", String.class);
        assertThat(versions).contains("001", "002", "003", "004", "005", "006");
    }

    @Test
    @DisplayName("Valida registro de usuario, BCrypt UTF-8 e persistencia real no PostgreSQL")
    void validaRegistroEHashBcrypt() {
        RegisterRequest request = new RegisterRequest("Usuario Postgres", "postgres_user@teste.com", "SenhaForte123!");
        var response = authService.register(request);

        assertThat(response).isNotNull();
        assertThat(response.email()).isEqualTo("postgres_user@teste.com");

        Usuario usuario = usuarioRepository.findByEmailIgnoreCase("postgres_user@teste.com").orElseThrow();
        assertThat(usuario.getSenha()).startsWith("$2a$").hasSize(60);
        assertThat(usuario.getFailedLoginAttempts()).isZero();
        assertThat(usuario.getTokenVersion()).isZero();
        assertThat(usuario.getLockedUntil()).isNull();
    }

    @Test
    @DisplayName("Valida transacoes financeiras (Gasto, Recebimento, Investimento) e Dashboard no PostgreSQL")
    void validaTransacoesFinanceirasEDashboard() {
        // Criar usuario
        authService.register(new RegisterRequest("Finan User", "finan@teste.com", "SenhaForte123!"));
        Usuario usuario = usuarioRepository.findByEmailIgnoreCase("finan@teste.com").orElseThrow();

        // Autenticar no contexto de seguranca
        autenticarComo(usuario);

        // Cadastrar Gasto
        gastoService.cadastrar(new GastoRequest("Mercado", "Alimentacao", new BigDecimal("150.00"),
                LocalDate.now(), "Debito", "Compras"));

        // Cadastrar Recebimento
        recebimentoService.cadastrar(new RecebimentoRequest("Salario", "Empresa", new BigDecimal("5000.00"),
                LocalDate.now(), "Recebido"));

        // Cadastrar Investimento
        investimentoService.cadastrar(new InvestimentoRequest("Tesouro Direto", "Renda Fixa", "Banco",
                new BigDecimal("1000.00"), new BigDecimal("0.90"), LocalDate.now()));

        // Verificar persistencia no banco
        assertThat(gastoRepository.count()).isEqualTo(1);
        assertThat(recebimentoRepository.count()).isEqualTo(1);
        assertThat(investimentoRepository.count()).isEqualTo(1);

        // Verificar calculo do Dashboard: 5000 - 150 + 1000 = 5850
        var dashboard = dashboardService.consolidar();
        assertThat(dashboard.getTotalRecebimentos()).isEqualByComparingTo("5000.00");
        assertThat(dashboard.getTotalGastos()).isEqualByComparingTo("150.00");
        assertThat(dashboard.getTotalInvestimentos()).isEqualByComparingTo("1000.00");
        assertThat(dashboard.getSaldo()).isEqualByComparingTo("5850.00");
    }

    @Test
    @DisplayName("Valida isolamento multi-usuario e protecao anti-IDOR no PostgreSQL")
    void validaIsolamentoMultiUsuario() {
        authService.register(new RegisterRequest("User A", "usera@teste.com", "SenhaForte123!"));
        Usuario userA = usuarioRepository.findByEmailIgnoreCase("usera@teste.com").orElseThrow();

        authService.register(new RegisterRequest("User B", "userb@teste.com", "SenhaForte123!"));
        Usuario userB = usuarioRepository.findByEmailIgnoreCase("userb@teste.com").orElseThrow();

        // User A cria um gasto
        autenticarComo(userA);
        var gastoA = gastoService.cadastrar(new GastoRequest("Gasto User A", "Pessoal", new BigDecimal("80.00"),
                LocalDate.now(), "Pix", null));

        // User B tenta listar gastos (deve retornar vazio)
        autenticarComo(userB);
        List<?> gastosB = gastoService.listar();
        assertThat(gastosB).isEmpty();

        // User B tenta buscar o gasto do User A por ID (deve lancar excecao / 404)
        assertThatThrownBy(() -> gastoService.buscarPorId(gastoA.codigo()))
                .hasMessageContaining("Recurso nao encontrado");

        // User B tenta deletar o gasto do User A
        assertThatThrownBy(() -> gastoService.deletar(gastoA.codigo()))
                .hasMessageContaining("Recurso nao encontrado");
    }

    private void autenticarComo(Usuario usuario) {
        var userDetails = br.com.emr.emrfinancas.security.AuthenticatedUserDetails.from(usuario);
        var auth = new UsernamePasswordAuthenticationToken(userDetails, null,
                List.of(new SimpleGrantedAuthority("ROLE_USER")));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}
