package br.com.emr.emrfinancas;

import br.com.emr.emrfinancas.model.Usuario;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.ResourcePropertySource;
import org.springframework.jdbc.datasource.AbstractDataSource;
import org.springframework.mock.env.MockEnvironment;
import static org.assertj.core.api.Assertions.assertThat;

class ConnectionPoolStartupTests {
    @TempDir Path temporaryDirectory;

    @Test
    void productionPoolReusesItsInitialPhysicalConnection() throws Exception {
        var env = new MockEnvironment();
        env.getPropertySources().addLast(new ResourcePropertySource(new ClassPathResource("application-prod.properties")));
        var source = new CountingDataSource();
        var config = config(source, Integer.parseInt(env.getProperty("spring.datasource.hikari.minimum-idle")));
        try (var pool = new HikariDataSource(config); var connection = pool.getConnection()) {
            assertThat(connection.isValid(1)).isTrue();
            assertThat(source.opened.get()).as("initial connection must be reused, not discarded").isEqualTo(1);
        }
    }

    @Test
    void zeroMinimumIdleRequiresTwoPhysicalConnectionsOnFirstBorrow() throws Exception {
        var source = new CountingDataSource();
        try (var pool = new HikariDataSource(config(source, 0)); var connection = pool.getConnection()) {
            assertThat(connection.isValid(1)).isTrue();
            assertThat(source.opened.get()).isEqualTo(2);
        }
    }

    @Test
    void closedIdleConnectionIsReplacedOnDemandWithoutKeepalive() throws Exception {
        var source = new CountingDataSource();
        try (var pool = new HikariDataSource(config(source, 1))) {
            try (var connection = pool.getConnection()) { assertThat(connection.isValid(1)).isTrue(); }
            source.lastConnection.close(); // Simulate remote server dropping an idle session.
            Thread.sleep(600); // Past Hikari's normal alive-check bypass window (500 ms).
            try (var connection = pool.getConnection(); var statement = connection.createStatement();
                 var result = statement.executeQuery("SELECT 1")) {
                assertThat(result.next()).isTrue();
                assertThat(result.getInt(1)).isEqualTo(1);
                assertThat(source.opened.get()).isGreaterThanOrEqualTo(2);
            }
            assertThat(pool.getKeepaliveTime()).isZero();
        }
    }

    @Test
    void unavailableDatabasePreventsContextStartup() {
        persistence("jdbc:h2:mem:not-created-" + UUID.randomUUID() + ";IFEXISTS=TRUE")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure()).hasStackTraceContaining("Database");
                });
    }

    @Test
    void incompatibleSchemaStillPreventsStartup() throws Exception {
        String url = migratedFixture();
        try (var connection = DriverManager.getConnection(url, "sa", ""); var sql = connection.createStatement()) {
            sql.execute("ALTER TABLE tb_emr_usuario DROP COLUMN dt_bloqueio_ate");
        }
        persistence(url).run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure()).hasStackTraceContaining("Schema validation");
        });
    }

    @Test
    void changedMigrationChecksumStillPreventsStartup() throws Exception {
        String url = migratedFixture();
        Path copies = temporaryDirectory.resolve("migrations");
        Files.createDirectories(copies);
        try (var files = Files.list(Path.of("src/main/resources/db/migration"))) {
            for (Path file : files.toList()) Files.copy(file, copies.resolve(file.getFileName()));
        }
        Path first = copies.resolve("V001__initial_schema.sql");
        Files.writeString(first, Files.readString(first).replace("START WITH 1", "START WITH 2"));
        persistence(url).withPropertyValues("spring.flyway.locations=filesystem:" + copies)
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure()).hasStackTraceContaining("checksum mismatch");
                });
    }

    private String migratedFixture() {
        String url = "jdbc:h2:mem:" + UUID.randomUUID()
                + ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";
        Flyway.configure().dataSource(url, "sa", "").load().migrate();
        return url;
    }

    private ApplicationContextRunner persistence(String url) {
        return new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(DataSourceAutoConfiguration.class,
                        FlywayAutoConfiguration.class, HibernateJpaAutoConfiguration.class))
                .withUserConfiguration(Entities.class)
                .withPropertyValues("spring.datasource.url=" + url,
                        "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa",
                        "spring.datasource.password=", "spring.datasource.hikari.maximum-pool-size=5",
                        "spring.datasource.hikari.minimum-idle=1", "spring.datasource.hikari.connection-timeout=250",
                        "spring.jpa.hibernate.ddl-auto=validate", "spring.flyway.enabled=true",
                        "spring.flyway.validate-on-migrate=true", "spring.flyway.clean-disabled=true",
                        "spring.flyway.baseline-on-migrate=false");
    }

    @Configuration(proxyBeanMethods = false)
    @EntityScan(basePackageClasses = Usuario.class)
    static class Entities { }

    private HikariConfig config(CountingDataSource source, int minimumIdle) {
        var config = new HikariConfig();
        config.setDataSource(source);
        config.setMaximumPoolSize(5);
        config.setMinimumIdle(minimumIdle);
        config.setKeepaliveTime(0);
        return config;
    }

    private static class CountingDataSource extends AbstractDataSource {
        private final AtomicInteger opened = new AtomicInteger();
        private final String url = "jdbc:h2:mem:" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1";
        private Connection lastConnection;

        @Override public Connection getConnection() throws SQLException {
            lastConnection = DriverManager.getConnection(url, "sa", "");
            opened.incrementAndGet();
            return lastConnection;
        }
        @Override public Connection getConnection(String username, String password) throws SQLException {
            return getConnection();
        }
    }
}
