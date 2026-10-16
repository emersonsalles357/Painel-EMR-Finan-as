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
            if (uri.getHost() == null || !uri.getHost().endsWith(".neon.tech") || uri.getUserInfo() != null
                    || uri.getHost().contains("-pooler.") || !uri.getHost().equals(required("DB_EXPECTED_HOST"))
                    || !"/emr_task5".equals(uri.getRawPath()) || (uri.getPort() != -1 && uri.getPort() != 5432)
                    || !"emr_task5_app".equals(required("DB_USERNAME")))
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
                try (Statement st = db.createStatement(); ResultSet rs = st.executeQuery(
                        "SELECT current_database()='emr_task5' AND current_user='emr_task5_app' AND session_user='emr_task5_app' "
                        + "AND current_setting('server_version_num')::int BETWEEN 160000 AND 169999 "
                        + "AND current_schema()='public' AND current_setting('transaction_read_only')='on'")) {
                    if (!rs.next() || !rs.getBoolean(1)) throw new IllegalStateException("Target identity rejected");
                }
                query(db, "SELECT 1 AS connectivity, current_database(), current_user, current_setting('transaction_read_only') AS read_only");
                query(db, "SELECT session_user,current_schema(),current_setting('search_path') AS search_path,current_setting('server_version') AS server_version");
                query(db, "SELECT ssl, version, cipher FROM pg_stat_ssl WHERE pid=pg_backend_pid()");
                query(db, "SELECT rolname,rolcanlogin,rolinherit,rolsuper,rolcreatedb,rolcreaterole,rolreplication,rolbypassrls FROM pg_roles WHERE rolname IN ('emr_task5_app','emr_task5_owner','emr_task5_migrator') ORDER BY rolname");
                query(db, "SELECT r.rolname AS member_of FROM pg_roles r WHERE r.rolname<>current_user AND pg_has_role(current_user,r.oid,'MEMBER') ORDER BY 1");
                query(db, "SELECT r.rolname AS granted_role,m.inherit_option,m.set_option,m.admin_option FROM pg_auth_members m JOIN pg_roles r ON r.oid=m.roleid JOIN pg_roles u ON u.oid=m.member WHERE u.rolname=current_user");
                query(db, "SELECT pg_get_userbyid(datdba) AS database_owner,datacl::text,has_database_privilege(current_user,oid,'CONNECT') AS connect,has_database_privilege(current_user,oid,'CREATE') AS create_database,has_database_privilege(current_user,oid,'TEMP') AS temporary FROM pg_database WHERE datname=current_database()");
                query(db, "SELECT nspname,pg_get_userbyid(nspowner) AS owner,nspacl::text,has_schema_privilege(current_user,oid,'USAGE') AS usage,has_schema_privilege(current_user,oid,'CREATE') AS create_objects FROM pg_namespace WHERE nspname !~ '^pg_' AND nspname<>'information_schema' ORDER BY 1");
                query(db, "SELECT n.nspname,c.relname,c.relkind,pg_get_userbyid(c.relowner) AS owner,c.relacl::text FROM pg_class c JOIN pg_namespace n ON n.oid=c.relnamespace WHERE n.nspname !~ '^pg_' AND n.nspname<>'information_schema' ORDER BY 1,2");
                query(db, "SELECT n.nspname,p.proname,pg_get_userbyid(p.proowner) AS owner,p.proacl::text FROM pg_proc p JOIN pg_namespace n ON n.oid=p.pronamespace WHERE n.nspname !~ '^pg_' AND n.nspname<>'information_schema' ORDER BY 1,2");
                query(db, "SELECT n.nspname,t.typname,pg_get_userbyid(t.typowner) AS owner FROM pg_type t JOIN pg_namespace n ON n.oid=t.typnamespace WHERE n.nspname !~ '^pg_' AND n.nspname<>'information_schema' ORDER BY 1,2");
                query(db, "SELECT pg_get_userbyid(defaclrole) AS creator,defaclnamespace::regnamespace,defaclobjtype,defaclacl::text FROM pg_default_acl ORDER BY 1,2");
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
