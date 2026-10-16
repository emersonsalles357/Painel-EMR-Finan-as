package br.com.emr.emrfinancas.security;

import br.com.emr.emrfinancas.model.Usuario;
import br.com.emr.emrfinancas.dto.UsuarioResponse;
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
    private final UsuarioResponse usuarioResponse;
    private final boolean loginFailures;

    private AuthenticatedUserDetails(String username, String password,
                                     List<GrantedAuthority> authorities, long tokenVersion,
                                     UsuarioResponse usuarioResponse, boolean loginFailures) {
        this.username = username;
        this.password = password;
        this.authorities = authorities;
        this.tokenVersion = tokenVersion;
        this.usuarioResponse = usuarioResponse;
        this.loginFailures = loginFailures;
    }

    public static AuthenticatedUserDetails from(Usuario usuario) {
        return new AuthenticatedUserDetails(
                usuario.getEmail(),
                usuario.getSenha(),
                List.of(new SimpleGrantedAuthority(usuario.getRole().authority())),
                usuario.getTokenVersion(), UsuarioResponse.from(usuario),
                usuario.getFailedLoginAttempts() > 0 || usuario.getLockedUntil() != null);
    }

    public UsuarioResponse getUsuarioResponse() { return usuarioResponse; }

    public boolean hasLoginFailures() { return loginFailures; }

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
