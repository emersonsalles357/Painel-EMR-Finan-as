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
    private MockEnvironment original() throws Exception {
        return valid().withProperty("DB_TARGET_ID", "neon-original")
                .withProperty("DB_URL", "jdbc:postgresql://ep-icy-cherry-acw6ysuv.sa-east-1.aws.neon.tech/neondb")
                .withProperty("DB_EXPECTED_HOST", "ep-icy-cherry-acw6ysuv.sa-east-1.aws.neon.tech")
                .withProperty("DB_USERNAME", "emr_financas_app");
    }

    @Test void acceptsExplicitOriginalIdentityWithRestrictedRole() throws Exception {
        ProductionConfigGuard.validate(original());
    }

    @Test void rejectsUnknownTargetEvenWithValidTask5Settings() throws Exception {
        assertThatThrownBy(() -> ProductionConfigGuard.validate(valid().withProperty("DB_TARGET_ID", "unknown")))
                .isInstanceOf(IllegalStateException.class);
    }

    @ParameterizedTest
    @CsvSource({"DB_USERNAME,neondb_owner", "DB_USERNAME,emr_task5_app",
        "DB_TARGET_ID,task5", "DB_EXPECTED_HOST,ep-other.neon.tech",
        "DB_URL,jdbc:postgresql://ep-other.neon.tech/neondb",
        "DB_URL,jdbc:postgresql://ep-icy-cherry-acw6ysuv.sa-east-1.aws.neon.tech/emr_task5",
        "DB_URL,jdbc:postgresql://ep-icy-cherry-acw6ysuv.sa-east-1.aws.neon.tech/neondb?sslmode=disable",
        "spring.flyway.enabled,true", "spring.jpa.hibernate.ddl-auto,update",
        "MIGRATION_DB_PASSWORD,forbidden", "DB_SSL_MODE,require"})
    void rejectsOriginalIdentityOverrides(String key, String value) throws Exception {
        assertThatThrownBy(() -> ProductionConfigGuard.validate(original().withProperty(key, value)))
                .isInstanceOf(IllegalStateException.class);
    }

    private MockEnvironment valid() throws Exception {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("prod");
        env.getPropertySources().addLast(new ResourcePropertySource(new ClassPathResource("application-prod.properties")));
        env.getPropertySources().addLast(new ResourcePropertySource(new ClassPathResource("application.properties")));
        return env.withProperty("DB_URL", "jdbc:postgresql://ep-test.neon.tech/emr_task5")
                .withProperty("DB_EXPECTED_HOST", "ep-test.neon.tech")
                .withProperty("DB_USERNAME", "emr_task5_app").withProperty("DB_PASSWORD", "test-only")
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

    @Test void respectsPortProvidedByHostingPlatform() throws Exception {
        MockEnvironment env = new MockEnvironment().withProperty("PORT", "10000");
        env.getPropertySources().addLast(new ResourcePropertySource(new ClassPathResource("application.properties")));
        assertThat(env.getProperty("server.port")).isEqualTo("10000");
    }

    @Test void acceptsRuntimeWithFlywayDisabled() throws Exception {
        ProductionConfigGuard.validate(valid().withProperty("spring.flyway.enabled", "false"));
    }

    @Test void rejectsFlywayInHttpProcess() throws Exception {
        assertThatThrownBy(() -> ProductionConfigGuard.validate(valid().withProperty("spring.flyway.enabled", "true")))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test void rejectsWrongDatabaseAndPrivilegedIdentity() throws Exception {
        assertThatThrownBy(() -> ProductionConfigGuard.validate(valid()
                .withProperty("DB_URL", "jdbc:postgresql://ep-test.neon.tech/another_database")))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> ProductionConfigGuard.validate(valid().withProperty("DB_USERNAME", "emr_task5_owner")))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test void rejectsEndpointDifferentFromApprovedHost() throws Exception {
        assertThatThrownBy(() -> ProductionConfigGuard.validate(valid()
                .withProperty("DB_EXPECTED_HOST", "ep-another.neon.tech")))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test void rejectsMigrationCredentialsAndAlternateDatasourceOverrides() throws Exception {
        for (String key : new String[]{"MIGRATION_DB_PASSWORD", "spring.flyway.password",
                "spring.datasource.hikari.jdbc-url", "spring.datasource.hikari.data-source-properties.password"}) {
            assertThatThrownBy(() -> ProductionConfigGuard.validate(valid().withProperty(key, "forbidden")))
                    .as(key).isInstanceOf(IllegalStateException.class);
        }
    }
}
