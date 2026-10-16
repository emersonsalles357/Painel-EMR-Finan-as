package br.com.emr.emrfinancas;

import br.com.emr.emrfinancas.model.Usuario;
import br.com.emr.emrfinancas.security.AuthenticatedUserDetails;
import java.io.*;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class AuthenticatedUserDetailsTests {
    @Test
    void principalRetainsSerializableContractWithPublicSnapshot() throws Exception {
        var user = new Usuario();
        user.setCodigo(42L);
        user.setNome("Synthetic User");
        user.setEmail("serialization@example.invalid");
        user.setSenha("synthetic-hash");
        user.invalidateTokens();
        var principal = AuthenticatedUserDetails.from(user);
        var bytes = new ByteArrayOutputStream();
        try (var output = new ObjectOutputStream(bytes)) {
            output.writeObject(principal);
        }
        AuthenticatedUserDetails restored;
        try (var input = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            restored = (AuthenticatedUserDetails) input.readObject();
        }
        assertThat(restored.getUsuarioResponse()).isEqualTo(principal.getUsuarioResponse());
        assertThat(restored.getUsername()).isEqualTo(principal.getUsername());
        assertThat(restored.getPassword()).isEqualTo(principal.getPassword());
        assertThat(restored.getAuthorities()).isEqualTo(principal.getAuthorities());
        assertThat(restored.getTokenVersion()).isEqualTo(principal.getTokenVersion());
        assertThat(restored.hasLoginFailures()).isEqualTo(principal.hasLoginFailures());
    }
}
