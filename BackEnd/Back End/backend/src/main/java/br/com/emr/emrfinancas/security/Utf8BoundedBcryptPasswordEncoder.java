package br.com.emr.emrfinancas.security;

import java.util.Objects;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/** Preserves the BCrypt format and refuses inputs the algorithm would truncate. */
public final class Utf8BoundedBcryptPasswordEncoder implements PasswordEncoder {
    // Public synthetic fixture; cost 10 matches the configured default. Never a user's hash.
    private static final String COMPENSATION_PASSWORD = "synthetic-length-check";
    private static final String COMPENSATION_HASH = "$2a$10$l7SxsX5dDp9fOVBY6jdDi.AaXkyqdQ2xUOuKehuGMhAdkRs2FdxWC";
    private final BCryptPasswordEncoder delegate;

    public Utf8BoundedBcryptPasswordEncoder(BCryptPasswordEncoder delegate) {
        this.delegate = Objects.requireNonNull(delegate);
    }

    @Override
    public String encode(CharSequence rawPassword) {
        String raw = rawPassword == null ? null : rawPassword.toString();
        if (!PasswordByteLimits.isAcceptableForBcrypt(raw)) {
            throw new IllegalArgumentException("Senha fora do limite UTF-8 do BCrypt");
        }
        return delegate.encode(raw);
    }

    @Override
    public boolean matches(CharSequence rawPassword, String encodedPassword) {
        String raw = rawPassword == null ? null : rawPassword.toString();
        if (!PasswordByteLimits.isAcceptableForBcrypt(raw)) {
            // Keep one real BCrypt operation even when DaoAuthenticationProvider masks an unknown user.
            delegate.matches(COMPENSATION_PASSWORD, COMPENSATION_HASH);
            return false;
        }
        return delegate.matches(raw, encodedPassword);
    }

    @Override
    public boolean upgradeEncoding(String encodedPassword) {
        return false;
    }
}
