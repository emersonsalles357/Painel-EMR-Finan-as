package br.com.emr.emrfinancas.database;

import java.sql.SQLException;
import java.util.Map;
import org.flywaydb.core.Flyway;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

/** One-shot entry point: no Spring context, HTTP server, JPA or runtime credentials. */
public final class DatabaseMigration {
    private DatabaseMigration() { }

    public static void main(String[] args) {
        try {
            if (args.length > 1 || (args.length == 1 && !args[0].equals("--validate-only"))) throw new IllegalArgumentException();
            Map<String, String> env = System.getenv();
            if (env.keySet().stream().anyMatch(k -> k.startsWith("SPRING_DATASOURCE_") || k.startsWith("SPRING_FLYWAY_")
                    || k.startsWith("FLYWAY_") || k.equals("SPRING_APPLICATION_JSON") || k.equals("DB_PASSWORD")))
                throw new IllegalStateException("Mixed migration/runtime environment");
            String mode = required(env, "MIGRATION_MODE");
            String url = required(env, "MIGRATION_DB_URL");
            if (!DatabaseTarget.MIGRATION_ROLE.equals(required(env, "MIGRATION_DB_USERNAME"))) throw new IllegalStateException();
            DatabaseTarget target = switch (mode) {
                case "neon" -> DatabaseTarget.neon(url, required(env, "MIGRATION_EXPECTED_HOST"), DatabaseTarget.MIGRATION_ROLE);
                case "local" -> DatabaseTarget.disposable(url, DatabaseTarget.MIGRATION_ROLE, required(env, "MIGRATION_LOCAL_MARKER"));
                default -> throw new IllegalStateException();
            };
            var source = new DriverManagerDataSource(target.url(), target.properties(required(env, "MIGRATION_DB_PASSWORD")));
            try (var connection = source.getConnection()) {
                target.verifyIdentity(connection);
                try (var query = connection.createStatement(); var row = query.executeQuery("SELECT has_schema_privilege(current_user,'public','CREATE')")) {
                    if (!row.next() || !row.getBoolean(1)) throw new IllegalStateException();
                }
            }
            Flyway flyway = Flyway.configure().dataSource(source).locations("classpath:db/migration")
                    .defaultSchema("public").schemas("public").createSchemas(false)
                    .baselineOnMigrate(false).cleanDisabled(true).validateOnMigrate(true)
                    .loggers(QuietLogCreator.class.getName()).load();
            int executed = args.length == 0 ? flyway.migrate().migrationsExecuted : 0;
            flyway.validate();
            var applied = flyway.info().applied();
            if (applied.length != 6 || flyway.info().pending().length != 0) throw new IllegalStateException();
            for (var migration : applied) {
                if (!migration.getState().isApplied() || migration.getChecksum() == null) throw new IllegalStateException();
                System.out.printf("migration version=%s checksum=%s state=%s%n", migration.getVersion(), migration.getChecksum(), migration.getState());
            }
            System.out.printf("migration-complete executed=%d validated=%d http=false%n", executed, applied.length);
        } catch (Exception ex) {
            // Flyway/JDBC exception messages may contain endpoints. Never echo their values.
            System.err.println("Migration failed: " + ex.getClass().getSimpleName()
                    + (ex instanceof SQLException sql ? " SQLState=" + sql.getSQLState() : ""));
            System.exit(1);
        }
    }

    private static String required(Map<String, String> env, String key) {
        String value = env.get(key);
        if (value == null || value.isBlank()) throw new IllegalStateException("Missing migration setting: " + key);
        return value;
    }

    /** Flyway messages include JDBC endpoints; emit only controlled migration results above. */
    public static final class QuietLogCreator implements org.flywaydb.core.api.logging.LogCreator {
        @Override public org.flywaydb.core.api.logging.Log createLogger(Class<?> type) {
            return new org.flywaydb.core.api.logging.Log() {
                public void debug(String message) { }
                public void info(String message) { }
                public void warn(String message) { System.err.println("Migration warning (details withheld)"); }
                public void error(String message) { System.err.println("Migration error (details withheld)"); }
                public void error(String message, Exception error) { error(message); }
                public void notice(String message) { }
            };
        }
    }
}
