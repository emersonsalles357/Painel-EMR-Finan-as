package br.com.emr.emrfinancas;

import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringBootVersion;
import static org.junit.jupiter.api.Assertions.*;

class DependencyMigrationContractTests {
    @Test
    void runtimeDeclaresApprovedStack() {
        assertAll(
                () -> assertEquals("4.1.1", SpringBootVersion.getVersion(), "approved checkpoint"),
                () -> assertEquals(17, Runtime.version().feature(), "Java 17 validation runtime"),
                () -> assertEquals("0.13.0", Jwts.class.getPackage().getImplementationVersion(),
                        "JJWT signing/parsing family must remain unchanged"));
    }
}
