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
    private final java.time.Clock clock;

    public AuthService(UsuarioRepository usuarioRepository, AuthenticationManager authenticationManager,
                       JwtService jwtService, AuthenticatedUserService authenticatedUserService,
                       PasswordEncoder passwordEncoder, PasswordPolicyValidator passwordPolicyValidator,
                       EmailNormalizer emailNormalizer, java.time.Clock clock) {
        this.usuarioRepository = usuarioRepository;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.authenticatedUserService = authenticatedUserService;
        this.passwordEncoder = passwordEncoder;
        this.passwordPolicyValidator = passwordPolicyValidator;
        this.emailNormalizer = emailNormalizer;
        this.clock = clock;
    }

    public LoginResponse login(LoginRequest loginRequest) {
        String normalizedEmail = emailNormalizer.normalize(loginRequest.getEmail());
        java.time.Instant now = clock.instant();

        java.util.Optional<Usuario> usuarioOpt = usuarioRepository.findByEmailIgnoreCase(normalizedEmail);
        if (usuarioOpt.isPresent()) {
            Usuario usuario = usuarioOpt.get();
            if (usuario.isAccountLocked(now)) {
                throw new org.springframework.security.authentication.BadCredentialsException("Credenciais invalidas.");
            }
            if (usuario.getLockedUntil() != null && !usuario.isAccountLocked(now)) {
                usuario.resetLockout();
                usuarioRepository.save(usuario);
            }
        }

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(normalizedEmail, loginRequest.getSenha()));
            UserDetails principal = (UserDetails) authentication.getPrincipal();
            Usuario usuario = usuarioRepository.findByEmailIgnoreCase(principal.getUsername()).orElseThrow();

            if (usuario.getFailedLoginAttempts() > 0 || usuario.getLockedUntil() != null) {
                usuario.resetLockout();
                usuarioRepository.save(usuario);
            }

            return new LoginResponse(jwtService.generateToken(principal), jwtService.getExpirationSeconds(),
                    UsuarioResponse.from(usuario));
        } catch (org.springframework.security.core.AuthenticationException exception) {
            if (usuarioOpt.isPresent()) {
                Usuario usuario = usuarioOpt.get();
                usuario.registerFailedLoginAttempt(now, 3, 15);
                usuarioRepository.save(usuario);
            }
            throw new org.springframework.security.authentication.BadCredentialsException("Credenciais invalidas.", exception);
        }
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
