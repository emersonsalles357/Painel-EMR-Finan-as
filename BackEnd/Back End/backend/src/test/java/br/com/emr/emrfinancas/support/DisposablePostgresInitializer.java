package br.com.emr.emrfinancas.support;

import br.com.emr.emrfinancas.database.DatabaseTarget;
import java.sql.DriverManager;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

/** Reject targets before Spring creates Flyway/JPA/datasource beans. */
public class DisposablePostgresInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
    @Override public void initialize(ConfigurableApplicationContext context) {
        var env = context.getEnvironment();
        var target = DatabaseTarget.disposable(env.getRequiredProperty("EMR_LOCAL_PG_URL"),
                DatabaseTarget.APPLICATION_ROLE, env.getRequiredProperty("EMR_LOCAL_PG_MARKER"));
        try (var connection = DriverManager.getConnection(target.url(), target.properties(env.getRequiredProperty("EMR_LOCAL_PG_PASSWORD")))) {
            connection.setReadOnly(true);
            target.verifyIdentity(connection);
        } catch (Exception ex) {
            throw new IllegalStateException("Disposable PostgreSQL identity rejected");
        }
    }
}
