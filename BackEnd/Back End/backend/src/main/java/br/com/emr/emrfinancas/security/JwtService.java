package br.com.emr.emrfinancas.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;

@Service
public class JwtService {
    private static final String TOKEN_VERSION_CLAIM = "tokenVersion";

    private final SecretKey signingKey;
    private final long expirationMillis;
    private final JwtParser parser;

    public JwtService(@Value("${app.jwt.secret}") String secret,
                      @Value("${app.jwt.expiration-ms:3600000}") long expirationMillis) {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalArgumentException("JWT_SECRET deve possuir pelo menos 32 bytes");
        }
        if (expirationMillis <= 0) {
            throw new IllegalArgumentException("JWT_EXPIRATION deve ser maior que zero");
        }
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMillis = expirationMillis;
        this.parser = Jwts.parser().verifyWith(signingKey).build();
    }

    public String generateToken(UserDetails userDetails) {
        return generateToken(userDetails, Instant.now(), expirationMillis);
    }

    public String generateToken(UserDetails userDetails, Instant issuedAt, long lifetimeMillis) {
        List<String> authorities = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();
        return Jwts.builder()
                .subject(userDetails.getUsername())
                .claim("authorities", authorities)
                .claim(TOKEN_VERSION_CLAIM, tokenVersionOf(userDetails))
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(issuedAt.plusMillis(lifetimeMillis)))
                .signWith(signingKey)
                .compact();
    }

    public String extractUsername(String token) {
        return parseToken(token).getSubject();
    }

    public boolean isValid(String token, UserDetails userDetails) {
        return isValid(parseToken(token), userDetails);
    }

    public VerifiedToken parseToken(String token) {
        Claims claims = parser.parseSignedClaims(token).getPayload();
        if (claims.getSubject() == null || claims.getExpiration() == null) {
            throw new JwtException("Token invalido.");
        }
        return new VerifiedToken(this, claims.getSubject(), tokenVersionFrom(claims),
                claims.getExpiration().toInstant());
    }

    public boolean isValid(VerifiedToken token, UserDetails userDetails) {
        return token != null && token.verifier == this
                && userDetails.getUsername().equals(token.subject)
                && token.tokenVersion == tokenVersionOf(userDetails)
                && token.expiration.isAfter(Instant.now());
    }

    public long getExpirationSeconds() {
        return expirationMillis / 1000;
    }

    /** Only this service can create verified claims; no mutable Claims or cross-request cache. */
    public static final class VerifiedToken {
        private final JwtService verifier;
        private final String subject;
        private final long tokenVersion;
        private final Instant expiration;

        private VerifiedToken(JwtService verifier, String subject, long tokenVersion, Instant expiration) {
            this.verifier = verifier;
            this.subject = subject;
            this.tokenVersion = tokenVersion;
            this.expiration = expiration;
        }

        public String getSubject() { return subject; }
    }

    private long tokenVersionOf(UserDetails userDetails) {
        if (userDetails instanceof AuthenticatedUserDetails authenticatedUser) {
            return authenticatedUser.getTokenVersion();
        }
        return 0;
    }

    private long tokenVersionFrom(Claims claims) {
        Object value = claims.get(TOKEN_VERSION_CLAIM);
        if (value == null) {
            return 0;
        }
        return value instanceof Number number ? number.longValue() : -1;
    }
}
