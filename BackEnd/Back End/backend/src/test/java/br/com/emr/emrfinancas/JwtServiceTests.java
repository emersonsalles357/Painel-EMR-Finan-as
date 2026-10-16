package br.com.emr.emrfinancas;

import br.com.emr.emrfinancas.model.Usuario;
import br.com.emr.emrfinancas.security.*;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class JwtServiceTests {
    private static final String SECRET = "test-only-jwt-secret-with-at-least-32-bytes";
    private final JwtService jwt = spy(new JwtService(SECRET, 3600000));

    @AfterEach void clearSecurityContext() { SecurityContextHolder.clearContext(); }

    @Test
    void filterVerifiesSignatureOnceAndLoadsCurrentUser() throws Exception {
        var user = user();
        var principal = AuthenticatedUserDetails.from(user);
        String token = jwt.generateToken(principal);
        var details = mock(CustomUserDetailsService.class); // Storage boundary; cryptography is real.
        when(details.loadUserByUsername(user.getEmail())).thenReturn(principal);
        var request = new MockHttpServletRequest("GET", "/api/auth/me");
        request.addHeader("Authorization", "Bearer " + token);
        new JwtAuthenticationFilter(jwt, details).doFilter(request, new MockHttpServletResponse(), (req, res) -> { });

        assertThat(SecurityContextHolder.getContext().getAuthentication().getName()).isEqualTo(user.getEmail());
        verify(details).loadUserByUsername(user.getEmail());
        long parses = mockingDetails(jwt).getInvocations().stream().filter(invocation -> {
            var method = invocation.getMethod();
            return method.getName().equals("parseToken") || method.getName().equals("extractUsername")
                    || (method.getName().equals("isValid") && method.getParameterTypes()[0] == String.class);
        }).count();
        assertThat(parses).as("one cryptographic parse in the filter").isEqualTo(1);
    }

    @Test
    void signedTokenWithoutExpirationIsRejectedAsJwtError() {
        String token = Jwts.builder().subject(user().getEmail())
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8))).compact();
        assertThatThrownBy(() -> jwt.isValid(token, AuthenticatedUserDetails.from(user())))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void expiredAndTamperedTokensRemainRejected() {
        var principal = AuthenticatedUserDetails.from(user());
        String expired = jwt.generateToken(principal, Instant.now().minusSeconds(7200), 3600000);
        assertThatThrownBy(() -> jwt.isValid(expired, principal)).isInstanceOf(JwtException.class);
        String token = jwt.generateToken(principal);
        int start = token.lastIndexOf('.') + 1;
        String tampered = token.substring(0, start) + (token.charAt(start) == 'a' ? 'b' : 'a') + token.substring(start + 1);
        assertThatThrownBy(() -> jwt.isValid(tampered, principal)).isInstanceOf(JwtException.class);
    }

    @Test
    void missingTokenVersionRetainsCompatibilityOnlyForVersionZero() {
        var user = user();
        String token = signed(user.getEmail(), null);
        assertThat(jwt.isValid(token, AuthenticatedUserDetails.from(user))).isTrue();
        user.invalidateTokens();
        assertThat(jwt.isValid(token, AuthenticatedUserDetails.from(user))).isFalse();
    }

    @Test
    void invalidVersionAndDifferentSubjectCannotAuthenticate() {
        var principal = AuthenticatedUserDetails.from(user());
        assertThat(jwt.isValid(signed(principal.getUsername(), "invalid"), principal)).isFalse();
        assertThat(jwt.isValid(signed("other@example.invalid", 0), principal)).isFalse();
    }

    @Test
    void verifiedClaimsCannotBeReusedByAnotherVerifier() {
        var principal = AuthenticatedUserDetails.from(user());
        var other = new JwtService("another-test-only-signing-key-at-least-32-bytes", 3600000);
        var verified = jwt.parseToken(jwt.generateToken(principal));
        assertThat(other.isValid(verified, principal)).isFalse();
    }

    @Test
    void missingSubjectNeverLoadsAUser() throws Exception {
        var details = mock(CustomUserDetailsService.class);
        var request = new MockHttpServletRequest("GET", "/api/auth/me");
        request.addHeader("Authorization", "Bearer " + signed(null, 0));
        new JwtAuthenticationFilter(jwt, details).doFilter(request, new MockHttpServletResponse(), (req, res) -> { });
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verifyNoInteractions(details);
    }

    private Usuario user() {
        var user = new Usuario();
        user.setCodigo(1L);
        user.setNome("Synthetic JWT fixture");
        user.setEmail("jwt@example.invalid");
        user.setSenha("unused-by-jwt");
        return user;
    }

    private String signed(String subject, Object version) {
        var builder = Jwts.builder().subject(subject).expiration(Date.from(Instant.now().plusSeconds(3600)));
        if (version != null) builder.claim("tokenVersion", version);
        return builder.signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8))).compact();
    }
}
