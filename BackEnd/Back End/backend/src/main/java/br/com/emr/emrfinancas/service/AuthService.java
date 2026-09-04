package br.com.emr.emrfinancas.service;

import br.com.emr.emrfinancas.dto.LoginRequest;
import br.com.emr.emrfinancas.dto.LoginResponse;
import br.com.emr.emrfinancas.dto.UsuarioResponse;
import br.com.emr.emrfinancas.model.Usuario;
import br.com.emr.emrfinancas.repository.UsuarioRepository;
import br.com.emr.emrfinancas.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    private final UsuarioRepository usuarioRepository;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final AuthenticatedUserService authenticatedUserService;

    public AuthService(UsuarioRepository usuarioRepository, AuthenticationManager authenticationManager,
                       JwtService jwtService, AuthenticatedUserService authenticatedUserService) {
        this.usuarioRepository = usuarioRepository;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.authenticatedUserService = authenticatedUserService;
    }

    public LoginResponse login(LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getEmail(), loginRequest.getSenha()));
        UserDetails principal = (UserDetails) authentication.getPrincipal();
        Usuario usuario = usuarioRepository.findByEmail(principal.getUsername()).orElseThrow();
        return new LoginResponse(jwtService.generateToken(principal), jwtService.getExpirationSeconds(),
                UsuarioResponse.from(usuario));
    }

    public UsuarioResponse usuarioAutenticado() {
        return UsuarioResponse.from(authenticatedUserService.getAuthenticatedUser());
    }
}
