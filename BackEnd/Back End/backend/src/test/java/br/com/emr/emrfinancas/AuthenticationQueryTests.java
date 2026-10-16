package br.com.emr.emrfinancas;

import br.com.emr.emrfinancas.dto.LoginRequest;
import br.com.emr.emrfinancas.model.Usuario;
import br.com.emr.emrfinancas.model.UserRole;
import br.com.emr.emrfinancas.repository.UsuarioRepository;
import br.com.emr.emrfinancas.security.CustomUserDetailsService;
import br.com.emr.emrfinancas.security.JwtService;
import br.com.emr.emrfinancas.service.*;
import jakarta.persistence.EntityManagerFactory;
import java.time.Clock;
import java.time.Instant;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.MockMvcPrint;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Intentionally no @Transactional: SQL reads and committed failure counters must be real.
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:authentication_queries;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.jpa.properties.hibernate.generate_statistics=true",
        "logging.level.org.hibernate.stat=OFF",
        "logging.level.org.hibernate.engine.internal.StatisticalLoggingSessionEventListener=OFF"})
@AutoConfigureMockMvc(print = MockMvcPrint.NONE)
@ActiveProfiles("test")
class AuthenticationQueryTests {
    @Autowired MockMvc mvc;
    @Autowired UsuarioRepository repository;
    @Autowired PasswordEncoder encoder;
    @Autowired EntityManagerFactory emf;
    @Autowired CustomUserDetailsService details;
    @Autowired JwtService jwt;
    @Autowired AuthenticatedUserService authenticatedUsers;
    @Autowired PasswordPolicyValidator policy;
    @Autowired EmailNormalizer normalizer;
    @Autowired Clock clock;
    private Usuario user;

    @BeforeEach
    void seed() {
        repository.deleteAll();
        user = new Usuario();
        user.setNome("Synthetic query fixture");
        user.setEmail("queries@example.invalid");
        user.setSenha(encoder.encode("Test-only@123"));
        user = repository.saveAndFlush(user);
    }

    @Test
    void validLoginUsesOnlyTwoUserQueries() throws Exception {
        var statistics = emf.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();
        login("Test-only@123", 200);
        assertThat(statistics.getQueryExecutionCount()).as("user lookup count, excluding fixture setup").isEqualTo(2);
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(2);
    }

    @Test
    void failedLoginCommitsCounterOutsideTheRequestTransaction() throws Exception {
        login("incorrect", 401);
        assertThat(repository.findById(user.getCodigo()).orElseThrow().getFailedLoginAttempts()).isEqualTo(1);
    }

    @Test
    void incorrectPasswordAfterExpiredLockoutStartsANewFailureSequence() throws Exception {
        user.setFailedLoginAttempts(3);
        user.setLockedUntil(Instant.now().minusSeconds(60));
        repository.saveAndFlush(user);
        login("incorrect", 401);
        var stored = repository.findById(user.getCodigo()).orElseThrow();
        assertThat(stored.getFailedLoginAttempts()).isEqualTo(1);
        assertThat(stored.getLockedUntil()).isNull();
    }

    @Test
    void successResetDoesNotOverwritePasswordRoleOrRevocationChangedAfterAuthentication() {
        user.setFailedLoginAttempts(2);
        repository.saveAndFlush(user);
        String replacementHash = encoder.encode("Changed-after-auth@123");
        var service = new AuthService(repository, request -> {
            var principal = details.loadUserByUsername(request.getName());
            var concurrent = repository.findById(user.getCodigo()).orElseThrow();
            concurrent.setSenha(replacementHash);
            concurrent.setRole(UserRole.ADMIN);
            concurrent.invalidateTokens();
            repository.saveAndFlush(concurrent);
            return new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        }, jwt, authenticatedUsers, encoder, policy, normalizer, clock);

        service.login(new LoginRequest(user.getEmail(), "Test-only@123"));

        var stored = repository.findById(user.getCodigo()).orElseThrow();
        assertThat(stored.getFailedLoginAttempts()).isZero();
        assertThat(stored.getLockedUntil()).isNull();
        assertThat(stored.getSenha()).isEqualTo(replacementHash);
        assertThat(stored.getRole()).isEqualTo(UserRole.ADMIN);
        assertThat(stored.getTokenVersion()).isEqualTo(1);
    }

    private void login(String password, int status) throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"queries@example.invalid\",\"senha\":\"" + password + "\"}"))
                .andExpect(status().is(status));
    }
}
