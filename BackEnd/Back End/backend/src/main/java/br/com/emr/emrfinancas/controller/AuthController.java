package br.com.emr.emrfinancas.controller;

import br.com.emr.emrfinancas.dto.LoginRequest;
import br.com.emr.emrfinancas.dto.LoginResponse;
import br.com.emr.emrfinancas.dto.UsuarioResponse;
import br.com.emr.emrfinancas.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
        return ResponseEntity.ok(authService.login(loginRequest));
    }

    @GetMapping("/me")
    public ResponseEntity<UsuarioResponse> me() {
        return ResponseEntity.ok(authService.usuarioAutenticado());
    }
}
