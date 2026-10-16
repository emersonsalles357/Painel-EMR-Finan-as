package br.com.emr.emrfinancas;

import br.com.emr.emrfinancas.exception.RegraNegocioException;
import br.com.emr.emrfinancas.service.PasswordPolicyValidator;
import br.com.emr.emrfinancas.support.PasswordHttpSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;

class PasswordBytePolicyTests {
    private final PasswordPolicyValidator policy = new PasswordPolicyValidator();
    @Test void byteFixturesIndependentlySatisfyCompositionAndExpectedUtf8Sizes() {
        for (String raw : Stream.concat(allowed(), excessive()).toList()) {
            assertTrue(raw.length() >= 10);
            assertTrue(raw.chars().anyMatch(Character::isUpperCase));
            assertTrue(raw.chars().anyMatch(Character::isLowerCase));
            assertTrue(raw.chars().anyMatch(Character::isDigit));
            assertTrue(raw.chars().anyMatch(c -> !Character.isLetterOrDigit(c) && !Character.isWhitespace(c)));
            int bytes = raw.getBytes(java.nio.charset.StandardCharsets.UTF_8).length;
            assertEquals(PasswordHttpSupport.excessiveByteFixtures().contains(raw) ? 73 : raw.equals("Aa1!"+"x".repeat(67)) ? 71 : 72, bytes);
        }
    }
    static Stream<String> allowed() { return PasswordHttpSupport.validByteFixtures().stream(); }
    static Stream<String> excessive() { return PasswordHttpSupport.excessiveByteFixtures().stream(); }
    static Stream<String> malformed() {
        return Stream.of("Aa1!xxxxxx\ud800", "Aa1!xxxxxx\udc00", "Aa1!xxxxxx\ud800x", "Aa1!xxxxxx\udc00\ud800");
    }
    @ParameterizedTest(name="{index}") @MethodSource("allowed")
    void acceptsCompositionValidBoundary(String raw) { assertDoesNotThrow(() -> policy.validate(raw)); }
    @ParameterizedTest(name="{index}") @MethodSource("excessive")
    void rejectsCompositionValidExcessBytes(String raw) { assertThrows(RegraNegocioException.class, () -> policy.validate(raw)); }
    @ParameterizedTest(name="{index}") @MethodSource("malformed")
    void rejectsMalformedUtf16(String raw) { assertThrows(RegraNegocioException.class, () -> policy.validate(raw)); }
    @Test void keepsMinimumAndComposition() {
        for (String raw : new String[]{"Aa1!short", "aaaaaaaa1!xx", "AAAAAAAA1!XX", "Aaaaaaaa!xx", "Aaaaaaaa1xx"}) {
            assertThrows(RegraNegocioException.class, () -> policy.validate(raw));
        }
    }
}
