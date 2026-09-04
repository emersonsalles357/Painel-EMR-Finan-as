package br.com.emr.emrfinancas.controller;

import br.com.emr.emrfinancas.dto.UsuarioResponse;
import br.com.emr.emrfinancas.model.Usuario;
import br.com.emr.emrfinancas.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {
    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping
    public ResponseEntity<List<UsuarioResponse>> listar() {
        return ResponseEntity.ok(usuarioService.listar());
    }

    @GetMapping("/{codigo}")
    public ResponseEntity<UsuarioResponse> buscarPorId(@PathVariable Long codigo) {
        return ResponseEntity.ok(usuarioService.buscarPorId(codigo));
    }

    @PostMapping
    public ResponseEntity<UsuarioResponse> cadastrar(@Valid @RequestBody Usuario usuario) {
        return ResponseEntity.status(HttpStatus.CREATED).body(usuarioService.cadastrar(usuario));
    }

    @PutMapping("/{codigo}")
    public ResponseEntity<UsuarioResponse> atualizar(@PathVariable Long codigo, @Valid @RequestBody Usuario usuario) {
        return ResponseEntity.ok(usuarioService.atualizar(codigo, usuario));
    }

    @DeleteMapping("/{codigo}")
    public ResponseEntity<Void> deletar(@PathVariable Long codigo) {
        usuarioService.deletar(codigo);
        return ResponseEntity.noContent().build();
    }
}
