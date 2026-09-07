package br.com.emr.emrfinancas;

import br.com.emr.emrfinancas.config.ProductionConfigGuard;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.ResourcePropertySource;
import org.springframework.mock.env.MockEnvironment;
import static org.assertj.core.api.Assertions.*;

class ProductionConfigurationTests {
    private MockEnvironment valid() throws Exception {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("prod");
        env.getPropertySources().addLast(new ResourcePropertySource(new ClassPathResource("application-prod.properties")));
        env.getPropertySources().addLast(new ResourcePropertySource(new ClassPathResource("application.properties")));
        return env.withProperty("DB_URL", "jdbc:postgresql://ep-test.neon.tech/test")
                .withProperty("DB_USERNAME", "test-user").withProperty("DB_PASSWORD", "test-only")
                .withProperty("FRONTEND_URL", "https://app.example.com")
                .withProperty("JWT_SECRET", "test-only-0123456789-ABCDEFGHIJKLMNOPQRSTUVWXYZ")
                .withProperty("MAIL_HOST", "smtp.example.com").withProperty("MAIL_USERNAME", "test")
                .withProperty("MAIL_PASSWORD", "test-only").withProperty("MAIL_FROM", "test@example.com");
    }

    @Test void acceptsSecureProductionAndSeparateCorsOrigins() throws Exception {
        ProductionConfigGuard.validate(valid().withProperty("CORS_ALLOWED_ORIGINS",
                "https://app.example.com,https://staging.example.com"));
    }

    @ParameterizedTest
    @CsvSource({
        "DB_URL,jdbc:postgresql://ep-test.neon.tech/test?sslmode=disable",
        "DB_URL,jdbc:postgresql://user:secret@ep-test.neon.tech/test",
        "DB_URL,jdbc:postgresql://localhost/test",
        "DB_SSL_MODE,require", "MAIL_AUTH,false", "MAIL_STARTTLS,false",
        "FRONTEND_URL,http://localhost:5173", "FRONTEND_URL,https://app.example.com/path",
        "CORS_ALLOWED_ORIGINS,*", "JWT_SECRET,aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
        "JWT_EXPIRATION,3600001", "spring.flyway.baseline-on-migrate,true",
        "spring.jpa.hibernate.ddl-auto,update", "server.forward-headers-strategy,framework"
    })
    void rejectsUnsafeProductionWithoutEchoingValues(String key, String value) throws Exception {
        MockEnvironment env = valid().withProperty(key, value);
        assertThatThrownBy(() -> ProductionConfigGuard.validate(env))
                .isInstanceOf(IllegalStateException.class).hasMessageNotContaining(value);
    }

    @Test void rejectsMissingMailPassword() throws Exception {
        MockEnvironment env = valid().withProperty("MAIL_PASSWORD", "");
        assertThatThrownBy(() -> ProductionConfigGuard.validate(env)).isInstanceOf(IllegalStateException.class);
    }

    @Test void rejectsMixedProfiles() throws Exception {
        MockEnvironment env = valid();
        env.setActiveProfiles("prod", "dev");
        assertThatThrownBy(() -> ProductionConfigGuard.validate(env)).isInstanceOf(IllegalStateException.class);
    }

    @Test void devIgnoresProductionDatabaseVariables() throws Exception {
        MockEnvironment env = new MockEnvironment().withProperty("DB_URL", "jdbc:postgresql://production/test");
        env.getPropertySources().addLast(new ResourcePropertySource(new ClassPathResource("application-dev.properties")));
        assertThat(env.getProperty("spring.datasource.url")).isEqualTo("jdbc:postgresql://localhost:5432/emr_financas");
    }
}
