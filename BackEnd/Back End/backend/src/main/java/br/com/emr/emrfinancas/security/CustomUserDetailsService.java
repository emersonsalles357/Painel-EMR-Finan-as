package br.com.emr.emrfinancas.security;

import br.com.emr.emrfinancas.model.Usuario;
import br.com.emr.emrfinancas.repository.UsuarioRepository;
import br.com.emr.emrfinancas.service.EmailNormalizer;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {
    private final UsuarioRepository usuarioRepository;
    private final EmailNormalizer emailNormalizer;

    public CustomUserDetailsService(UsuarioRepository usuarioRepository, EmailNormalizer emailNormalizer) {
        this.usuarioRepository = usuarioRepository;
        this.emailNormalizer = emailNormalizer;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository.findByEmailIgnoreCase(emailNormalizer.normalize(email))
                .orElseThrow(() -> new UsernameNotFoundException("Credenciais invalidas."));
        return AuthenticatedUserDetails.from(usuario);
    }
}
