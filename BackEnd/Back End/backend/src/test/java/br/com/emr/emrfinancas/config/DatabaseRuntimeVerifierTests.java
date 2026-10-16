package br.com.emr.emrfinancas.config;

import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.env.MockEnvironment;
import static org.assertj.core.api.Assertions.*;

class DatabaseRuntimeVerifierTests {
    @ParameterizedTest
    @ValueSource(strings={"url", "username", "tls", "schema", "init"})
    void originalRejectsEffectivePoolOverridesBeforeConnecting(String change) {
        String host = "ep-icy-cherry-acw6ysuv.sa-east-1.aws.neon.tech";
        String url = "jdbc:postgresql://" + host + "/neondb";
        var env = new MockEnvironment().withProperty("DB_TARGET_ID", "neon-original")
                .withProperty("DB_EXPECTED_HOST", host).withProperty("spring.datasource.url", url)
                .withProperty("spring.datasource.username", "emr_financas_app")
                .withProperty("spring.flyway.enabled", "false").withProperty("spring.jpa.hibernate.ddl-auto", "validate");
        env.setActiveProfiles("prod");
        try (var source = new NeverConnect()) {
            source.setJdbcUrl(change.equals("url") ? "jdbc:postgresql://other.neon.tech/neondb" : url);
            source.setUsername(change.equals("username") ? "neondb_owner" : "emr_financas_app");
            source.addDataSourceProperty("sslmode", change.equals("tls") ? "require" : "verify-full");
            source.addDataSourceProperty("sslfactory", "org.postgresql.ssl.DefaultJavaSSLFactory");
            source.addDataSourceProperty("currentSchema", change.equals("schema") ? "other" : "public");
            if (change.equals("init")) source.setConnectionInitSql("CREATE TABLE forbidden(id int)");
            assertThatThrownBy(() -> DatabaseRuntimeVerifier.restrictedDatabaseIdentity(env)
                    .postProcessAfterInitialization(source, "dataSource")).isInstanceOf(IllegalStateException.class);
        }
    }

    @ParameterizedTest
    @ValueSource(strings={"url", "tls", "schema", "init"})
    void rejectsEffectivePoolOverridesBeforeOpeningConnection(String change) {
        String url = "jdbc:postgresql://127.0.0.1:61999/emr_task5_it_012345abcdef";
        var env = new MockEnvironment().withProperty("spring.datasource.url", url)
                .withProperty("EMR_LOCAL_PG_MARKER", "emr-task5-disposable:0123456789abcdef0123456789abcdef")
                .withProperty("spring.flyway.enabled", "false").withProperty("spring.jpa.hibernate.ddl-auto", "validate");
        env.setActiveProfiles("local-pg");
        try (var source = new NeverConnect()) {
            source.setJdbcUrl(change.equals("url") ? "jdbc:postgresql://forbidden.invalid/emr_task5" : url);
            source.setUsername("emr_task5_app");
            source.addDataSourceProperty("sslmode", change.equals("tls") ? "require" : "disable");
            source.addDataSourceProperty("currentSchema", change.equals("schema") ? "other" : "public");
            if (change.equals("init")) source.setConnectionInitSql("CREATE TABLE forbidden(id int)");
            assertThatThrownBy(() -> DatabaseRuntimeVerifier.restrictedDatabaseIdentity(env)
                    .postProcessAfterInitialization(source, "dataSource")).isInstanceOf(IllegalStateException.class);
        }
    }
    private static class NeverConnect extends HikariDataSource {
        @Override public Connection getConnection() { throw new AssertionError("Connection must not be opened"); }
    }
}
