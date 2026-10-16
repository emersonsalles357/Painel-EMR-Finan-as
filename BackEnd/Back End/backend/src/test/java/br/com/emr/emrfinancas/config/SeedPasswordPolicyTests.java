package br.com.emr.emrfinancas.config;

import br.com.emr.emrfinancas.exception.RegraNegocioException;
import br.com.emr.emrfinancas.service.EmailNormalizer;
import br.com.emr.emrfinancas.support.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;

class SeedPasswordPolicyTests extends PasswordHttpSupport {
    @Autowired AutowireCapableBeanFactory factory;
    @Autowired EmailNormalizer normalizer;
    static Stream<String> invalid() { return Stream.of("Aa1!", LegacyBcryptFixtures.UNICODE_73); }
    @ParameterizedTest(name="{index}") @MethodSource("invalid")
    void optInDevSeedRejectsInvalidPasswordWithoutWrites(String raw) {
        DataLoader loader=factory.createBean(DataLoader.class);
        assertThrows(RegraNegocioException.class,() -> loader.seedUsuario(users,encoder,normalizer,"Seed Sintetico","seed@test.invalid",raw).run());
        assertEquals(0,users.count());
    }
    @Test void seedAcceptsValidBoundaryAndRemainsIdempotent() throws Exception {
        DataLoader loader=factory.createBean(DataLoader.class);
        var runner=loader.seedUsuario(users,encoder,normalizer,"Seed Sintetico","seed@test.invalid",LegacyBcryptFixtures.BOUNDARY);
        runner.run(); runner.run(); assertEquals(1,users.count());
        assertTrue(encoder.matches(LegacyBcryptFixtures.BOUNDARY,users.findAll().get(0).getSenha()));
    }
}
