package br.com.emr.emrfinancas;

import br.com.emr.emrfinancas.exception.GlobalExceptionHandler;
import br.com.emr.emrfinancas.model.Usuario;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityHardeningTests {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void naoSerializaSenhaDoUsuario() throws Exception {
        Usuario usuario = new Usuario();
        usuario.setNome("Usuario de teste");
        usuario.setEmail("teste@example.com");
        usuario.setSenha("segredo");

        String json = objectMapper.writeValueAsString(usuario);

        assertThat(json).doesNotContain("senha", "segredo");
    }

    @Test
    void naoDevolveDetalheDeExcecaoInterna() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();

        ResponseEntity<Map<String, Object>> response = handler.tratarErroGeral(
                new RuntimeException("erro SQL sensivel"));

        assertThat(response.getBody())
                .containsEntry("mensagem", "Ocorreu um erro interno inesperado");
        assertThat(response.getBody().toString()).doesNotContain("SQL", "sensivel");
    }
}
