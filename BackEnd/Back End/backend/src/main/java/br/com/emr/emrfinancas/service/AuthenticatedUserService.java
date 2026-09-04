package br.com.emr.emrfinancas.service;

import br.com.emr.emrfinancas.model.Usuario;
import br.com.emr.emrfinancas.repository.UsuarioRepository;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class AuthenticatedUserService {
    private final UsuarioRepository usuarioRepository;

    public AuthenticatedUserService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    public Usuario getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication.getName() == null || authentication.getName().isBlank()) {
            throw new AuthenticationCredentialsNotFoundException("Autenticacao obrigatoria.");
        }

        return usuarioRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new AuthenticationCredentialsNotFoundException("Autenticacao invalida."));
    }
}
