package br.com.emr.emrfinancas.security;

import br.com.emr.emrfinancas.model.Usuario;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public final class AuthenticatedUserDetails implements UserDetails {
    private final String username;
    private final String password;
    private final List<GrantedAuthority> authorities;
    private final long tokenVersion;

    private AuthenticatedUserDetails(String username, String password,
                                     List<GrantedAuthority> authorities, long tokenVersion) {
        this.username = username;
        this.password = password;
        this.authorities = authorities;
        this.tokenVersion = tokenVersion;
    }

    public static AuthenticatedUserDetails from(Usuario usuario) {
        return new AuthenticatedUserDetails(
                usuario.getEmail(),
                usuario.getSenha(),
                List.of(new SimpleGrantedAuthority(usuario.getRole().authority())),
                usuario.getTokenVersion());
    }

    public long getTokenVersion() {
        return tokenVersion;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
