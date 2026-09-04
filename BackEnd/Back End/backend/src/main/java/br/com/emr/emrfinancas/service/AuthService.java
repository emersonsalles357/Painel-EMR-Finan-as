package br.com.emr.emrfinancas.service;

import br.com.emr.emrfinancas.dto.LoginRequest;
import br.com.emr.emrfinancas.dto.LoginResponse;
import br.com.emr.emrfinancas.dto.RegisterRequest;
import br.com.emr.emrfinancas.dto.RegisterResponse;
import br.com.emr.emrfinancas.dto.UsuarioResponse;
import br.com.emr.emrfinancas.exception.EmailJaCadastradoException;
import br.com.emr.emrfinancas.model.UserRole;
import br.com.emr.emrfinancas.model.Usuario;
import br.com.emr.emrfinancas.repository.UsuarioRepository;
import br.com.emr.emrfinancas.security.JwtService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    private final UsuarioRepository usuarioRepository;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final AuthenticatedUserService authenticatedUserService;
    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicyValidator passwordPolicyValidator;
    private final EmailNormalizer emailNormalizer;

    public AuthService(UsuarioRepository usuarioRepository, AuthenticationManager authenticationManager,
                       JwtService jwtService, AuthenticatedUserService authenticatedUserService,
                       PasswordEncoder passwordEncoder, PasswordPolicyValidator passwordPolicyValidator,
                       EmailNormalizer emailNormalizer) {
        this.usuarioRepository = usuarioRepository;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.authenticatedUserService = authenticatedUserService;
        this.passwordEncoder = passwordEncoder;
        this.passwordPolicyValidator = passwordPolicyValidator;
        this.emailNormalizer = emailNormalizer;
    }

    public LoginResponse login(LoginRequest loginRequest) {
        String normalizedEmail = emailNormalizer.normalize(loginRequest.getEmail());
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(normalizedEmail, loginRequest.getSenha()));
        UserDetails principal = (UserDetails) authentication.getPrincipal();
        Usuario usuario = usuarioRepository.findByEmailIgnoreCase(principal.getUsername()).orElseThrow();
        return new LoginResponse(jwtService.generateToken(principal), jwtService.getExpirationSeconds(),
                UsuarioResponse.from(usuario));
    }

    public RegisterResponse register(RegisterRequest request) {
        String normalizedEmail = emailNormalizer.normalize(request.getEmail());
        if (usuarioRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new EmailJaCadastradoException();
        }

        passwordPolicyValidator.validate(request.getSenha());

        Usuario usuario = new Usuario();
        usuario.setNome(request.getNome().trim());
        usuario.setEmail(normalizedEmail);
        usuario.setSenha(passwordEncoder.encode(request.getSenha()));
        usuario.setRole(UserRole.USER);

        try {
            return RegisterResponse.from(usuarioRepository.saveAndFlush(usuario));
        } catch (DataIntegrityViolationException exception) {
            throw new EmailJaCadastradoException();
        }
    }

    public UsuarioResponse usuarioAutenticado() {
        return UsuarioResponse.from(authenticatedUserService.getAuthenticatedUser());
    }
}
