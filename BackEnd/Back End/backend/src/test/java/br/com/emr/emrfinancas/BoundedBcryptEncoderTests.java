package br.com.emr.emrfinancas;

import br.com.emr.emrfinancas.support.LegacyBcryptFixtures;
import br.com.emr.emrfinancas.support.PasswordHttpSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;

class BoundedBcryptEncoderTests extends PasswordHttpSupport {
    static Stream<String> unacceptable() {
        return Stream.concat(excessiveByteFixtures().stream(), Stream.of("", "\ud800", "\udc00", "Aa1!" + "x".repeat(100000), null));
    }
    @ParameterizedTest(name="{index}") @MethodSource("unacceptable")
    void encodeRejectsUnacceptableInput(String raw) {
        assertThrows(IllegalArgumentException.class, () -> encoder.encode(raw));
    }
    @ParameterizedTest(name="{index}") @MethodSource("unacceptable")
    void matchesRejectsUnacceptableInputWithoutThrowing(String raw) {
        assertDoesNotThrow(() -> assertFalse(encoder.matches(raw, LegacyBcryptFixtures.BOUNDARY_HASH)));
    }
    @Test void legacyUnicodeExcessNeverAuthenticates() {
        assertFalse(encoder.matches(LegacyBcryptFixtures.UNICODE_73, LegacyBcryptFixtures.UNICODE_LEGACY_HASH));
    }
    @Test void refusesAutomaticHashUpgrade() { assertFalse(encoder.upgradeEncoding(LegacyBcryptFixtures.HASH_2A)); }
    @Test void realBcryptAcceptsCorrectAndRejectsIncorrectInBoundary() {
        for (String raw : validByteFixtures()) {
            String hash = encoder.encode(raw);
            assertTrue(encoder.matches(raw, hash));
            assertFalse(encoder.matches("ErradoAa1!xx", hash));
            assertTrue(hash.startsWith("$2a$10$"));
        }
    }
}
