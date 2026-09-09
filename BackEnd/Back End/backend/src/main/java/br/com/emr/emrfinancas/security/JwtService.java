package br.com.emr.emrfinancas.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
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
        return parseClaims(token).getSubject();
    }

    public boolean isValid(String token, UserDetails userDetails) {
        Claims claims = parseClaims(token);
        return userDetails.getUsername().equals(claims.getSubject())
                && tokenVersionFrom(claims) == tokenVersionOf(userDetails)
                && claims.getExpiration().after(new Date());
    }

    public long getExpirationSeconds() {
        return expirationMillis / 1000;
    }

    private Claims parseClaims(String token) {
        return Jwts.parser().verifyWith(signingKey).build().parseSignedClaims(token).getPayload();
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
