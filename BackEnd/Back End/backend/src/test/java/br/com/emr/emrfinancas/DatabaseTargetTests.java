package br.com.emr.emrfinancas;

import br.com.emr.emrfinancas.database.DatabaseTarget;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.assertj.core.api.Assertions.*;

class DatabaseTargetTests {
    private static final String MARKER = "emr-task5-disposable:0123456789abcdef0123456789abcdef";
    @Test void originalRuntimeRequiresPinnedIdentityAndPreservesTls() {
        String url = "jdbc:postgresql://" + DatabaseTarget.ORIGINAL_HOST + "/neondb";
        var target = DatabaseTarget.runtimeNeon("neon-original", url, DatabaseTarget.ORIGINAL_HOST, "emr_financas_app");
        assertThat(target.database()).isEqualTo("neondb");
        assertThat(target.properties("test-only")).containsEntry("sslmode", "verify-full")
                .containsEntry("currentSchema", "public")
                .containsEntry("sslfactory", "org.postgresql.ssl.DefaultJavaSSLFactory");
        assertThatThrownBy(() -> DatabaseTarget.neon(url, DatabaseTarget.ORIGINAL_HOST, DatabaseTarget.MIGRATION_ROLE))
                .isInstanceOf(IllegalStateException.class);
    }

    @ParameterizedTest
    @ValueSource(strings={"neondb_owner", "emr_task5_app", "emr_task5_migrator", ""})
    void originalRejectsOtherRoles(String role) {
        assertThatThrownBy(() -> DatabaseTarget.runtimeNeon("neon-original",
                "jdbc:postgresql://" + DatabaseTarget.ORIGINAL_HOST + "/neondb", DatabaseTarget.ORIGINAL_HOST, role))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test void localTransportRequiresDisposableLoopbackTarget() {
        var target = DatabaseTarget.disposable("jdbc:postgresql://127.0.0.1:61999/emr_task5_it_012345abcdef", DatabaseTarget.APPLICATION_ROLE, MARKER);
        assertThat(target.properties("test-only")).containsEntry("sslmode", "disable").containsEntry("currentSchema", "public");
    }
    @ParameterizedTest
    @ValueSource(strings={"jdbc:postgresql://ep-test.neon.tech/emr_task5", "jdbc:postgresql://example.onrender.com/emr_task5",
            "jdbc:postgresql://127.0.0.1:5432/emr_task5_it_012345abcdef", "jdbc:postgresql://127.0.0.1:61999/emr_financas",
            "jdbc:postgresql://localhost:61999/emr_task5_it_012345abcdef", "jdbc:postgresql://127.0.0.1:61999/emr_task5_it_012345abcdef?sslmode=disable"})
    void rejectsRemoteHistoricalAndAmbiguousDestinations(String url) {
        assertThatThrownBy(() -> DatabaseTarget.disposable(url, DatabaseTarget.APPLICATION_ROLE, MARKER)).isInstanceOf(IllegalStateException.class);
    }
    @Test void missingMarkerIsRejectedBeforeConnection() {
        assertThatThrownBy(() -> DatabaseTarget.disposable("jdbc:postgresql://127.0.0.1:61999/emr_task5_it_012345abcdef", DatabaseTarget.APPLICATION_ROLE, ""))
                .isInstanceOf(IllegalStateException.class);
    }
    @Test void neonAlwaysVerifiesHostnameAndCertificate() {
        var target = DatabaseTarget.neon("jdbc:postgresql://ep-test.neon.tech/emr_task5", "ep-test.neon.tech", DatabaseTarget.MIGRATION_ROLE);
        assertThat(target.properties("test-only")).containsEntry("sslmode", "verify-full")
                .containsEntry("sslfactory", "org.postgresql.ssl.DefaultJavaSSLFactory");
    }
    @Test void migrationRequiresDirectEndpoint() {
        assertThatThrownBy(() -> DatabaseTarget.neon("jdbc:postgresql://ep-test-pooler.neon.tech/emr_task5", "ep-test-pooler.neon.tech", DatabaseTarget.MIGRATION_ROLE))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test void neonVerifyIdentityAcceptsBackendProxyWithExternalVerifyFull() throws Exception {
        String url = "jdbc:postgresql://" + DatabaseTarget.ORIGINAL_HOST + "/neondb";
        var target = DatabaseTarget.runtimeNeon("neon-original", url, DatabaseTarget.ORIGINAL_HOST, "emr_financas_app");
        target.verifyIdentity(connectionStub("neondb", "emr_financas_app", 160015, true, false, true));
        assertThatThrownBy(() -> target.verifyIdentity(connectionStub("neondb", "emr_financas_app", 160015, false, false, true)))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> target.verifyIdentity(connectionStub("neondb", "emr_financas_app", 160015, true, false, false)))
                .isInstanceOf(IllegalStateException.class);
    }

    private static java.sql.Connection connectionStub(String db, String user, int version, boolean nonAdminRoleOk, boolean backendSsl, boolean hasSslRow) {
        return (java.sql.Connection) java.lang.reflect.Proxy.newProxyInstance(
                DatabaseTargetTests.class.getClassLoader(),
                new Class<?>[]{java.sql.Connection.class},
                (proxy, method, args) -> {
                    if ("createStatement".equals(method.getName())) {
                        return java.lang.reflect.Proxy.newProxyInstance(
                                DatabaseTargetTests.class.getClassLoader(),
                                new Class<?>[]{java.sql.Statement.class},
                                (stProxy, stMethod, stArgs) -> {
                                    if ("executeQuery".equals(stMethod.getName())) {
                                        String sql = (String) stArgs[0];
                                        return resultSetStub(sql, db, user, version, nonAdminRoleOk, backendSsl, hasSslRow);
                                    }
                                    return null;
                                });
                    }
                    return null;
                });
    }

    private static java.sql.ResultSet resultSetStub(String sql, String db, String user, int version, boolean nonAdminRoleOk, boolean backendSsl, boolean hasSslRow) {
        final boolean[] advanced = {false};
        return (java.sql.ResultSet) java.lang.reflect.Proxy.newProxyInstance(
                DatabaseTargetTests.class.getClassLoader(),
                new Class<?>[]{java.sql.ResultSet.class},
                (rsProxy, rsMethod, rsArgs) -> {
                    switch (rsMethod.getName()) {
                        case "next" -> {
                            if (advanced[0]) return false;
                            advanced[0] = true;
                            return !sql.contains("pg_stat_ssl") || hasSslRow;
                        }
                        case "getString" -> {
                            int col = (Integer) rsArgs[0];
                            if (col == 1) return db;
                            if (col == 2 || col == 3) return user;
                            if (col == 4) return "public";
                            return "";
                        }
                        case "getInt" -> { return version; }
                        case "getBoolean" -> {
                            if (sql.contains("pg_roles")) return nonAdminRoleOk;
                            if (sql.contains("pg_stat_ssl")) return backendSsl;
                            return false;
                        }
                        default -> { return null; }
                    }
                });
    }
}

