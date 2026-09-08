package br.com.emr.emrfinancas;

import br.com.emr.emrfinancas.controller.HealthController;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class HealthControllerTests {
    @Test
    void returnsOnlyGenericUpStatus() {
        var response = new HealthController().health();

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).containsExactlyEntriesOf(Map.of("status", "UP"));
    }
}
