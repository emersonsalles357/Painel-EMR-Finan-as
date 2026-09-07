package br.com.emr.emrfinancas.controller;

import br.com.emr.emrfinancas.dto.*;
import br.com.emr.emrfinancas.service.ProfileService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users/me")
public class ProfileController {
    private final ProfileService service;
    public ProfileController(ProfileService service) { this.service = service; }

    @PatchMapping
    public UsuarioResponse update(@Valid @RequestBody UpdateProfileRequest request) {
        return service.update(request);
    }

    @PostMapping("/password")
    public ResponseEntity<Void> password(@Valid @RequestBody ChangePasswordRequest request) {
        service.changePassword(request);
        return ResponseEntity.noContent().build();
    }
}
