package br.com.emr.emrfinancas;

import br.com.emr.emrfinancas.model.*;
import br.com.emr.emrfinancas.support.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import java.util.Map;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

class AdminPasswordPolicyIntegrationTests extends PasswordHttpSupport {
    static Stream<String> invalid() { return Stream.of("Aa1!", LegacyBcryptFixtures.UNICODE_73); }
    private String adminToken() throws Exception {
        return login(user("admin@test.invalid", LegacyBcryptFixtures.HASH_2B, UserRole.ADMIN), LegacyBcryptFixtures.NORMAL);
    }
    private Map<String,String> body(String raw) {
        return Map.of("nome","Novo Sintetico","email","new@test.invalid","senha",raw,"role","ADMIN");
    }
    private MvcResult update(Usuario user, String raw, String token) throws Exception {
        return mvc.perform(put("/api/usuarios/"+user.getCodigo()).contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(body(raw))).header("Authorization","Bearer "+token)).andReturn();
    }
    @ParameterizedTest(name="{index}") @MethodSource("invalid")
    void adminCannotCreateInvalidPasswordOrPartialAccount(String raw) throws Exception {
        String token=adminToken(); long count=users.count();
        MvcResult result=assertDoesNotThrow(() -> request("/api/usuarios",body(raw),token,"198.51.100.70"));
        status(result,400); assertEquals(count,users.count()); noSensitiveBody(result,raw);
    }
    @ParameterizedTest(name="{index}") @MethodSource("invalid")
    void adminCannotPartiallyUpdateNameEmailHashOrTokenVersion(String raw) throws Exception {
        String token=adminToken(); Usuario owner=owner(); String hash=owner.getSenha();
        MvcResult result=assertDoesNotThrow(() -> update(owner,raw,token));
        status(result,400); unchanged(owner,hash,0);
        assertTrue(saved(owner).getNome().equals(owner.getNome()));
        assertTrue(saved(owner).getEmail().equals(owner.getEmail())); noSensitiveBody(result,raw);
    }
    @Test void validAdminCreateKeepsUserRoleAndBoundaryHash() throws Exception {
        String token=adminToken(); status(request("/api/usuarios",body(LegacyBcryptFixtures.BOUNDARY),token,"198.51.100.70"),201);
        Usuario created=users.findByEmailIgnoreCase("new@test.invalid").orElseThrow();
        assertEquals(UserRole.USER,created.getRole()); assertTrue(encoder.matches(LegacyBcryptFixtures.BOUNDARY,created.getSenha()));
    }
    @Test void validAdminUpdateRevokesOldJwtAndPreservesAnotherUser() throws Exception {
        String token=adminToken(); Usuario owner=owner(); String old=login(owner,LegacyBcryptFixtures.BOUNDARY);
        Usuario other=user("other@test.invalid",LegacyBcryptFixtures.HASH_2B,UserRole.USER);
        status(update(owner,LegacyBcryptFixtures.NORMAL,token),200);
        assertEquals(1,saved(owner).getTokenVersion()); assertTrue(encoder.matches(LegacyBcryptFixtures.NORMAL,saved(owner).getSenha()));
        status(mvc.perform(get("/api/auth/me").header("Authorization","Bearer "+old)).andReturn(),401);
        unchanged(other,LegacyBcryptFixtures.HASH_2B,0);
    }
    @Test void ordinaryUserStillCannotAdministerAccounts() throws Exception {
        Usuario owner=owner(); String token=login(owner,LegacyBcryptFixtures.BOUNDARY);long count=users.count();
        status(request("/api/usuarios",body(LegacyBcryptFixtures.NORMAL),token,"198.51.100.70"),403);
        assertEquals(count,users.count());
    }
}
