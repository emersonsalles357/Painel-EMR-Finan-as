package br.com.emr.emrfinancas.database;

import java.sql.DriverManager;
import java.sql.SQLException;

/** One connection, READ ONLY, no Spring/Flyway/HTTP and no row data or credential output. */
public final class DatabaseRuntimePreflight {
    private DatabaseRuntimePreflight() { }

    public static void main(String[] args) {
        try {
            if (args.length != 0) throw new IllegalStateException();
            if (System.getenv().keySet().stream().anyMatch(k -> k.startsWith("MIGRATION_")
                    || k.startsWith("SPRING_") || k.startsWith("FLYWAY_"))) throw new IllegalStateException();
            String id = required("DB_TARGET_ID");
            if (!"neon-original".equals(id)) throw new IllegalStateException();
            DatabaseTarget target = DatabaseTarget.runtimeNeon(id, required("DB_URL"),
                    required("DB_EXPECTED_HOST"), required("DB_USERNAME"));
            var properties = target.properties(required("DB_PASSWORD"));
            properties.setProperty("options", "-c default_transaction_read_only=on");
            try (var connection = DriverManager.getConnection(target.url(), properties)) {
                connection.setReadOnly(true);
                connection.setAutoCommit(false);
                try {
                    try (var statement = connection.createStatement()) {
                        statement.setQueryTimeout(15);
                        try (var row = statement.executeQuery("SELECT current_setting('transaction_read_only')='on'")) {
                            if (!row.next() || !row.getBoolean(1)) throw new IllegalStateException();
                        }
                    }
                    target.verifyRuntime(connection);
                } finally {
                    connection.rollback();
                }
            }
            System.out.println("preflight=PASS authenticated=true tls=verify-full identity=approved privileges=restricted readOnly=true");
        } catch (Exception ex) {
            System.err.println("preflight=FAIL type=" + ex.getClass().getSimpleName()
                    + (ex instanceof SQLException sql ? " SQLState=" + sql.getSQLState() : ""));
            System.exit(1);
        }
    }

    private static String required(String key) {
        String value = System.getenv(key);
        if (value == null || value.isBlank()) throw new IllegalStateException();
        return value;
    }
}
