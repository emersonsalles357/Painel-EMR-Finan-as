package br.com.emr.emrfinancas.config;

import br.com.emr.emrfinancas.database.DatabaseTarget;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;

/** Checks the actual connection before the datasource can be consumed by Hibernate. */
@Configuration(proxyBeanMethods = false)
@Profile({"prod", "local-pg", "postgres-it"})
public class DatabaseRuntimeVerifier {
    @Bean
    static BeanPostProcessor restrictedDatabaseIdentity(Environment env) {
        return new BeanPostProcessor() {
            @Override public Object postProcessAfterInitialization(Object bean, String name) {
                if (bean instanceof DataSource source) {
                    boolean production = java.util.Arrays.asList(env.getActiveProfiles()).contains("prod");
                    DatabaseTarget target = production
                            ? DatabaseTarget.runtimeNeon(env.getProperty("DB_TARGET_ID", "task5"), env.getRequiredProperty("spring.datasource.url"),
                                    env.getRequiredProperty("DB_EXPECTED_HOST"), env.getRequiredProperty("spring.datasource.username"))
                            : DatabaseTarget.disposable(env.getRequiredProperty("spring.datasource.url"), DatabaseTarget.APPLICATION_ROLE, env.getRequiredProperty("EMR_LOCAL_PG_MARKER"));
                    if (!"false".equals(env.getProperty("spring.flyway.enabled"))
                            || !"validate".equals(env.getProperty("spring.jpa.hibernate.ddl-auto")))
                        throw new IllegalStateException("Runtime must not initialize schema");
                    for (String key : new String[]{"MIGRATION_DB_URL", "MIGRATION_DB_USERNAME", "MIGRATION_DB_PASSWORD",
                            "spring.flyway.url", "spring.flyway.user", "spring.flyway.password"}) {
                        if (env.getProperty(key) != null) throw new IllegalStateException("Migration settings forbidden in runtime");
                    }
                    verifyPool(source, target);
                    try (var connection = source.getConnection()) {
                        target.verifyRuntime(connection);
                    } catch (SQLException ex) {
                        throw new IllegalStateException("Runtime database verification failed SQLState=" + ex.getSQLState());
                    }
                }
                return bean;
            }
        };
    }

    private static void verifyPool(DataSource source, DatabaseTarget target) {
        if (!(source instanceof HikariDataSource pool)
                || !target.url().equals(pool.getJdbcUrl()) || !target.username().equals(pool.getUsername())
                || pool.getDataSource() != null || pool.getDataSourceClassName() != null || pool.getDataSourceJNDI() != null
                || pool.getConnectionInitSql() != null || pool.getConnectionTestQuery() != null || pool.getCatalog() != null
                || (pool.getSchema() != null && !pool.getSchema().equals("public")))
            throw new IllegalStateException("Unexpected effective runtime datasource");
        var properties = pool.getDataSourceProperties();
        var allowed = java.util.Set.of("sslmode", "sslfactory", "currentSchema", "connectTimeout", "socketTimeout");
        if (!allowed.containsAll(properties.stringPropertyNames()) || !"public".equals(properties.getProperty("currentSchema"))
                || !(target.local() ? "disable" : "verify-full").equals(properties.getProperty("sslmode"))
                || (!target.local() && !"org.postgresql.ssl.DefaultJavaSSLFactory".equals(properties.getProperty("sslfactory"))))
            throw new IllegalStateException("Unexpected effective JDBC transport or schema");
    }
}
