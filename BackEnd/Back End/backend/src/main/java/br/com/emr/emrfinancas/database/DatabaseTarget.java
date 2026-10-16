package br.com.emr.emrfinancas.database;

import java.net.URI;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

/** Closed target policy shared by migration, runtime and disposable integration tests. */
public record DatabaseTarget(String url, String database, String username, String marker, boolean local) {
    public static final String APPLICATION_ROLE = "emr_task5_app";
    public static final String MIGRATION_ROLE = "emr_task5_migrator";
    public static final String ORIGINAL_APPLICATION_ROLE = "emr_financas_app";
    public static final String ORIGINAL_HOST = "ep-icy-cherry-acw6ysuv.sa-east-1.aws.neon.tech";

    /** Explicit runtime opt-in; never used by the standalone migration entry point. */
    public static DatabaseTarget runtimeNeon(String id, String url, String expectedHost, String username) {
        if ("task5".equals(id)) {
            require(APPLICATION_ROLE.equals(username));
            return neon(url, expectedHost, username);
        }
        require("neon-original".equals(id));
        URI uri = parse(url);
        require(ORIGINAL_HOST.equals(expectedHost) && ORIGINAL_HOST.equals(uri.getHost()));
        require(uri.getPort() == -1 || uri.getPort() == 5432);
        require("/neondb".equals(uri.getRawPath()) && ORIGINAL_APPLICATION_ROLE.equals(username));
        return new DatabaseTarget(url, "neondb", username, "", false);
    }

    public static DatabaseTarget neon(String url, String expectedHost, String username) {
        URI uri = parse(url);
        require(expectedHost != null && expectedHost.matches("[a-z0-9-]+(?:\\.[a-z0-9-]+)*\\.neon\\.tech"));
        require(expectedHost.equals(uri.getHost()) && !expectedHost.contains("-pooler."));
        require(uri.getPort() == -1 || uri.getPort() == 5432);
        require("/emr_task5".equals(uri.getRawPath()));
        role(username);
        return new DatabaseTarget(url, "emr_task5", username, "", false);
    }

    public static DatabaseTarget disposable(String url, String username, String marker) {
        URI uri = parse(url);
        require("127.0.0.1".equals(uri.getHost()) && uri.getPort() > 1024 && uri.getPort() <= 65535 && uri.getPort() != 5432);
        require(uri.getRawPath() != null && uri.getRawPath().matches("/emr_task5_it_[a-f0-9]{12}"));
        require(marker != null && marker.matches("emr-task5-disposable:[a-f0-9]{32}"));
        role(username);
        return new DatabaseTarget(url, uri.getRawPath().substring(1), username, marker, true);
    }

    private static URI parse(String url) {
        require(url != null && url.startsWith("jdbc:postgresql://"));
        try {
            URI uri = URI.create(url.substring(5));
            require(uri.getUserInfo() == null && uri.getQuery() == null && uri.getFragment() == null && uri.getHost() != null);
            return uri;
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException("Invalid database target");
        }
    }

    private static void role(String username) {
        require(APPLICATION_ROLE.equals(username) || MIGRATION_ROLE.equals(username));
    }

    public Properties properties(String password) {
        require(password != null && !password.isBlank());
        Properties properties = new Properties();
        properties.setProperty("user", username);
        properties.setProperty("password", password);
        properties.setProperty("currentSchema", "public");
        properties.setProperty("sslmode", local ? "disable" : "verify-full");
        if (!local) properties.setProperty("sslfactory", "org.postgresql.ssl.DefaultJavaSSLFactory");
        properties.setProperty("connectTimeout", "15");
        properties.setProperty("socketTimeout", "30");
        return properties;
    }

    public void verifyIdentity(Connection connection) throws SQLException {
        try (var statement = connection.createStatement()) {
            statement.setQueryTimeout(15);
            try (var row = statement.executeQuery("SELECT current_database(),current_user,session_user,current_schema(),"
                    + "current_setting('server_version_num')::int,shobj_description(oid,'pg_database') "
                    + "FROM pg_database WHERE datname=current_database()")) {
                require(row.next() && database.equals(row.getString(1)) && username.equals(row.getString(2))
                        && username.equals(row.getString(3)) && "public".equals(row.getString(4))
                        && row.getInt(5) >= 160000 && row.getInt(5) < 170000);
                if (local) require(marker.equals(row.getString(6)));
            }
            try (var row = statement.executeQuery("SELECT rolcanlogin AND NOT (rolsuper OR rolcreatedb OR rolcreaterole OR rolreplication OR rolbypassrls) "
                    + "FROM pg_roles WHERE rolname=current_user")) {
                require(row.next() && row.getBoolean(1));
            }
            if (!local) {
                // External TLS verify-full is enforced by pgJDBC/DefaultJavaSSLFactory; Neon proxy terminates TLS before postgres backend.
                try (var row = statement.executeQuery("SELECT ssl FROM pg_stat_ssl WHERE pid=pg_backend_pid()")) {
                    require(row.next());
                }
            }
        }
    }

    public void verifyRuntime(Connection connection) throws SQLException {
        require(APPLICATION_ROLE.equals(username) || (!local && ORIGINAL_APPLICATION_ROLE.equals(username)
                && "neondb".equals(database) && ORIGINAL_HOST.equals(parse(url).getHost())));
        verifyIdentity(connection);
        try (var statement = connection.createStatement()) {
            statement.setQueryTimeout(15);
            try (var row = statement.executeQuery("SELECT has_database_privilege(current_user,current_database(),'CONNECT') "
                    + "AND NOT has_database_privilege(current_user,current_database(),'CREATE') "
                    + "AND NOT has_database_privilege(current_user,current_database(),'TEMP') "
                    + "AND has_schema_privilege(current_user,'public','USAGE') "
                    + "AND NOT has_schema_privilege(current_user,'public','CREATE') "
                    + "AND NOT EXISTS (SELECT 1 FROM pg_roles r WHERE r.rolname<>current_user AND pg_has_role(current_user,r.oid,'MEMBER')) "
                    + "AND NOT pg_has_role(current_user,(SELECT datdba FROM pg_database WHERE datname=current_database()),'MEMBER')")) {
                require(row.next() && row.getBoolean(1));
            }
            try (var row = statement.executeQuery("SELECT NOT EXISTS (SELECT 1 FROM pg_namespace n "
                    + "WHERE n.nspname !~ '^pg_' AND n.nspname<>'information_schema' "
                    + "AND (has_schema_privilege(current_user,n.oid,'CREATE') OR pg_has_role(current_user,n.nspowner,'MEMBER'))) "
                    + "AND NOT EXISTS (SELECT 1 FROM pg_proc p JOIN pg_namespace n ON n.oid=p.pronamespace "
                    + "WHERE n.nspname !~ '^pg_' AND n.nspname<>'information_schema' AND pg_has_role(current_user,p.proowner,'MEMBER')) "
                    + "AND NOT EXISTS (SELECT 1 FROM pg_type t JOIN pg_namespace n ON n.oid=t.typnamespace "
                    + "WHERE n.nspname !~ '^pg_' AND n.nspname<>'information_schema' AND pg_has_role(current_user,t.typowner,'MEMBER'))")) {
                require(row.next() && row.getBoolean(1));
            }
            try (var row = statement.executeQuery("SELECT NOT EXISTS (SELECT 1 FROM pg_class c JOIN pg_namespace n ON n.oid=c.relnamespace "
                    + "WHERE n.nspname !~ '^pg_' AND n.nspname<>'information_schema' AND (pg_has_role(current_user,c.relowner,'MEMBER') "
                    + "OR (c.relkind IN ('r','p') AND (has_table_privilege(current_user,c.oid,'TRUNCATE') "
                    + "OR has_table_privilege(current_user,c.oid,'TRIGGER') OR has_table_privilege(current_user,c.oid,'REFERENCES')))))")) {
                require(row.next() && row.getBoolean(1));
            }
            for (String table : new String[]{"tb_emr_usuario", "tb_emr_gasto", "tb_emr_recebimento", "tb_emr_investimento", "tb_emr_password_reset_token"}) {
                require(privilege(connection, table, "SELECT") && privilege(connection, table, "INSERT") && privilege(connection, table, "UPDATE"));
                if (!table.equals("tb_emr_password_reset_token")) require(privilege(connection, table, "DELETE"));
            }
            for (String sequence : new String[]{"seq_emr_usuario", "seq_emr_gasto", "seq_emr_recebimento", "seq_emr_investimento", "seq_emr_password_reset_token"}) {
                try (var query = connection.prepareStatement("SELECT has_sequence_privilege(current_user,?,'USAGE') AND NOT has_sequence_privilege(current_user,?,'UPDATE')")) {
                    query.setString(1, "public." + sequence); query.setString(2, "public." + sequence);
                    try (var row = query.executeQuery()) { require(row.next() && row.getBoolean(1)); }
                }
            }
            for (String privilege : new String[]{"SELECT", "INSERT", "UPDATE", "DELETE", "TRUNCATE", "REFERENCES", "TRIGGER"}) {
                require(!privilege(connection, "flyway_schema_history", privilege));
            }
        }
    }

    private static boolean privilege(Connection connection, String table, String privilege) throws SQLException {
        try (var query = connection.prepareStatement("SELECT has_table_privilege(current_user,?,?)")) {
            query.setString(1, "public." + table); query.setString(2, privilege);
            try (var row = query.executeQuery()) { return row.next() && row.getBoolean(1); }
        }
    }

    private static void require(boolean condition) {
        if (!condition) throw new IllegalStateException("Database identity or privileges rejected");
    }
}
