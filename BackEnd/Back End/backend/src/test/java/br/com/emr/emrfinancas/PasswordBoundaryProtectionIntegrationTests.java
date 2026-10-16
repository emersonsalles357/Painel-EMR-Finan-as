package br.com.emr.emrfinancas;

import br.com.emr.emrfinancas.model.Usuario;
import br.com.emr.emrfinancas.service.PasswordResetService;
import br.com.emr.emrfinancas.support.*;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

class PasswordBoundaryProtectionIntegrationTests extends PasswordHttpSupport {
    private Map<String,String> credentials(String email) { return Map.of("email",email,"senha",LegacyBcryptFixtures.BOUNDARY+"suffix"); }
    @Test void longPasswordStillAccumulatesFailuresLocksAndRecovers() throws Exception {
        Usuario owner=owner(); String hash=owner.getSenha();
        var existing=request("/api/auth/login",credentials(owner.getEmail()));
        var missing=request("/api/auth/login",credentials("missing@test.invalid"));
        status(existing,401);status(missing,401);
        var first=json.readTree(existing.getResponse().getContentAsString());
        var second=json.readTree(missing.getResponse().getContentAsString());
        ((tools.jackson.databind.node.ObjectNode)first).remove("timestamp");
        ((tools.jackson.databind.node.ObjectNode)second).remove("timestamp");
        assertTrue(first.equals(second),"same generic error contract");
        for(int attempts=2;attempts<=3;attempts++) {
            status(request("/api/auth/login",credentials(owner.getEmail())),401);
            assertEquals(attempts,saved(owner).getFailedLoginAttempts());
        }
        assertEquals(NOW.plusSeconds(900),saved(owner).getLockedUntil());
        status(request("/api/auth/login",Map.of("email",owner.getEmail(),"senha",LegacyBcryptFixtures.BOUNDARY)),401);
        when(clock.instant()).thenReturn(NOW.plusSeconds(901));
        login(owner,LegacyBcryptFixtures.BOUNDARY); assertEquals(0,saved(owner).getFailedLoginAttempts());
        assertNull(saved(owner).getLockedUntil()); unchanged(owner,hash,0); assertEquals(1,users.count());
    }
    @Test void longLoginCannotEvadeQuotaWithForgedForwardingHeaders() throws Exception {
        Usuario owner=owner();
        for(int i=0;i<6;i++) {
            var result=mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(credentials(owner.getEmail())))
                .header("X-Forwarded-For","203.0.113."+(i+1)).header("X-Real-IP","203.0.113."+(i+1))
                .with(req->{req.setRemoteAddr("198.51.100.71");return req;})).andReturn();
            status(result,i<5?401:429); noSensitiveBody(result,LegacyBcryptFixtures.BOUNDARY+"suffix");
        }
        assertEquals(3,saved(owner).getFailedLoginAttempts()); unchanged(owner,LegacyBcryptFixtures.BOUNDARY_HASH,0);
    }
    @Test void asciiExcessRegistrationCountsTowardQuota() throws Exception {
        String raw="Aa1!"+"x".repeat(69);
        for(int i=0;i<6;i++) {
            var result=request("/api/auth/register",Map.of("nome","Novo Sintetico","email","new@test.invalid","senha",raw),null,"198.51.100.72");
            status(result,i<5?400:429);noSensitiveBody(result,raw);
        }
        assertEquals(0,users.count());
    }
    @Test void rejectedResetKeepsTokenAndCountsTowardQuota() throws Exception {
        Usuario owner=owner();String token=resetToken(owner);
        for(int i=0;i<11;i++) {
            var result=request("/api/auth/reset-password",Map.of("token",token,"novaSenha",LegacyBcryptFixtures.UNICODE_73),null,"198.51.100.73");
            status(result,i<10?400:429);noSensitiveBody(result,token,LegacyBcryptFixtures.UNICODE_73);
        }
        unchanged(owner,LegacyBcryptFixtures.BOUNDARY_HASH,0);
        assertNull(resets.findByTokenHash(PasswordResetService.hashToken(token)).orElseThrow().getUsedAt());
    }
    @Test void recoveryQuotaRemainsEnabledAndMailIsMocked() throws Exception {
        owner(); for(int i=0;i<6;i++) status(request("/api/auth/forgot-password",Map.of("email","owner@test.invalid"),null,"198.51.100.74"),i<5?200:429);
        verify(emailService,times(5)).sendPasswordResetEmail(anyString(),anyString());
    }
    @Test void invalidTokenTakesPriorityOverPasswordPolicyAndCannotWrite() throws Exception {
        Usuario owner=owner(); String token=resetToken(owner);
        var first=request("/api/auth/reset-password",Map.of("token","invalid-synthetic-token","novaSenha",LegacyBcryptFixtures.UNICODE_73));
        var second=request("/api/auth/reset-password",Map.of("token","invalid-synthetic-token","novaSenha",LegacyBcryptFixtures.NORMAL));
        status(first,400);status(second,400);
        assertTrue(json.readTree(first.getResponse().getContentAsString()).get("message").equals(json.readTree(second.getResponse().getContentAsString()).get("message")));
        unchanged(owner,LegacyBcryptFixtures.BOUNDARY_HASH,0);
        assertNull(resets.findByTokenHash(PasswordResetService.hashToken(token)).orElseThrow().getUsedAt());
    }
    @Test void unauthenticatedChangeRemainsForbidden() throws Exception {
        Usuario owner=owner();status(request("/api/users/me/password",Map.of("senhaAtual",LegacyBcryptFixtures.BOUNDARY,"novaSenha",LegacyBcryptFixtures.NORMAL)),401);
        unchanged(owner,LegacyBcryptFixtures.BOUNDARY_HASH,0);
    }
}
