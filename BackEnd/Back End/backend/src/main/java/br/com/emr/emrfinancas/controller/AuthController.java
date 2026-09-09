package br.com.emr.emrfinancas.controller;

import br.com.emr.emrfinancas.dto.ForgotPasswordRequest;
import br.com.emr.emrfinancas.dto.ForgotPasswordResponse;
import br.com.emr.emrfinancas.dto.LoginRequest;
import br.com.emr.emrfinancas.dto.LoginResponse;
import br.com.emr.emrfinancas.dto.RegisterRequest;
import br.com.emr.emrfinancas.dto.RegisterResponse;
import br.com.emr.emrfinancas.dto.ResetPasswordRequest;
import br.com.emr.emrfinancas.dto.ResetPasswordResponse;
import br.com.emr.emrfinancas.dto.UsuarioResponse;
import br.com.emr.emrfinancas.security.ClientIpResolver;
import br.com.emr.emrfinancas.security.RateLimiterService;
import br.com.emr.emrfinancas.service.AuthService;
import br.com.emr.emrfinancas.service.PasswordResetService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;
    private final PasswordResetService passwordResetService;
    private final RateLimiterService rateLimiterService;
    private final ClientIpResolver clientIpResolver;

    public AuthController(AuthService authService,
                          PasswordResetService passwordResetService,
                          RateLimiterService rateLimiterService,
                          ClientIpResolver clientIpResolver) {
        this.authService = authService;
        this.passwordResetService = passwordResetService;
        this.rateLimiterService = rateLimiterService;
        this.clientIpResolver = clientIpResolver;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest loginRequest,
                                               HttpServletRequest httpRequest) {
        rateLimiterService.checkLoginRateLimit(clientIpResolver.resolve(httpRequest), loginRequest.getEmail());
        return ResponseEntity.ok(authService.login(loginRequest));
    }

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest registerRequest,
                                                     HttpServletRequest httpRequest) {
        rateLimiterService.checkRegisterRateLimit(clientIpResolver.resolve(httpRequest));
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(registerRequest));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ForgotPasswordResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request,
                                                                 HttpServletRequest httpRequest) {
        rateLimiterService.checkForgotPasswordRateLimit(clientIpResolver.resolve(httpRequest));
        return ResponseEntity.ok(passwordResetService.solicitarRecuperacao(request));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ResetPasswordResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest request,
                                                               HttpServletRequest httpRequest) {
        rateLimiterService.checkResetPasswordRateLimit(clientIpResolver.resolve(httpRequest));
        return ResponseEntity.ok(passwordResetService.redefinirSenha(request));
    }

    @GetMapping("/me")
    public ResponseEntity<UsuarioResponse> me() {
        return ResponseEntity.ok(authService.usuarioAutenticado());
    }
}
