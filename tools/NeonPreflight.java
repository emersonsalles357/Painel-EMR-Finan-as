import java.net.URI;
import java.sql.*;
import java.util.Properties;

/** Read-only Neon inspection. Never starts Spring or Flyway; never prints credentials or tokens. */
class NeonPreflight {
    static String required(String key) {
        String value = System.getenv(key);
        if (value == null || value.isBlank()) throw new IllegalArgumentException("Missing " + key);
        return value;
    }
    static void query(Connection db, String sql) throws SQLException {
        try (Statement st = db.createStatement()) {
            st.setQueryTimeout(30);
            try (ResultSet rs = st.executeQuery(sql)) {
                int size = rs.getMetaData().getColumnCount();
                while (rs.next()) {
                    for (int i = 1; i <= size; i++) {
                        if (i > 1) System.out.print(" | ");
                        System.out.print(rs.getMetaData().getColumnLabel(i) + "=" + rs.getString(i));
                    }
                    System.out.println();
                }
            }
        }
    }
    public static void main(String[] args) {
        try {
            String url = required("DB_URL");
            if (!url.startsWith("jdbc:postgresql://") || url.contains("?") || url.contains("#"))
                throw new IllegalArgumentException("DB_URL must be a JDBC URL without query parameters");
            URI uri = URI.create(url.substring(5));
            if (uri.getHost() == null || !uri.getHost().endsWith(".neon.tech") || uri.getUserInfo() != null)
                throw new IllegalArgumentException("Expected Neon hostname and separate credentials");
            if (!"verify-full".equals(System.getenv().getOrDefault("DB_SSL_MODE", "verify-full")))
                throw new IllegalArgumentException("DB_SSL_MODE must be verify-full");
            Properties p = new Properties();
            p.setProperty("user", required("DB_USERNAME"));
            p.setProperty("password", required("DB_PASSWORD"));
            p.setProperty("sslmode", "verify-full");
            p.setProperty("sslfactory", "org.postgresql.ssl.DefaultJavaSSLFactory");
            p.setProperty("connectTimeout", "15");
            p.setProperty("socketTimeout", "30");
            p.setProperty("options", "-c default_transaction_read_only=on");
            try (Connection db = DriverManager.getConnection(url, p)) {
                db.setReadOnly(true);
                db.setAutoCommit(false);
                query(db, "SELECT 1 AS connectivity, current_database(), current_user, current_setting('transaction_read_only') AS read_only");
                query(db, "SELECT ssl, version, cipher FROM pg_stat_ssl WHERE pid=pg_backend_pid()");
                query(db, "SELECT has_schema_privilege(current_user,'public','USAGE') AS schema_usage, has_schema_privilege(current_user,'public','CREATE') AS schema_create");
                query(db, "SELECT table_schema,table_name FROM information_schema.tables WHERE table_schema NOT IN ('pg_catalog','information_schema') ORDER BY 1,2");
                query(db, "SELECT table_name,column_name,data_type,is_nullable,column_default FROM information_schema.columns WHERE table_schema='public' ORDER BY table_name,ordinal_position");
                query(db, "SELECT tablename,indexname,indexdef FROM pg_indexes WHERE schemaname='public' ORDER BY 1,2");
                query(db, "SELECT conrelid::regclass AS table_name,conname,pg_get_constraintdef(oid) AS definition FROM pg_constraint WHERE connamespace='public'::regnamespace ORDER BY 1,2");
                query(db, "SELECT sequencename,start_value,increment_by,last_value FROM pg_sequences WHERE schemaname='public'");
                for (String table : new String[]{"tb_emr_usuario","tb_emr_gasto","tb_emr_recebimento","tb_emr_investimento","tb_emr_password_reset_token","flyway_schema_history"}) {
                    try (ResultSet rs = db.getMetaData().getTables(null,"public",table,new String[]{"TABLE"})) {
                        if (rs.next()) {
                            query(db, "SELECT '" + table + "' AS table_name,count(*) FROM public." + table);
                            if (table.equals("flyway_schema_history"))
                                query(db, "SELECT version,description,checksum,success FROM public.flyway_schema_history ORDER BY installed_rank");
                        }
                    }
                }
                db.rollback();
                System.out.println("Read-only preflight complete. No migrations applied. Review all schemas before proceeding.");
            }
        } catch (Exception ex) {
            // SQLException messages may include connection details; intentionally withhold them.
            System.err.println("Preflight failed: " + ex.getClass().getSimpleName()
                    + (ex instanceof SQLException sql ? " SQLState=" + sql.getSQLState() : " (check required settings)"));
            System.exit(1);
        }
    }
}
