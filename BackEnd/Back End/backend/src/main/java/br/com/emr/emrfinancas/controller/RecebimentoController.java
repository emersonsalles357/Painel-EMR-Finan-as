package br.com.emr.emrfinancas.controller;

import br.com.emr.emrfinancas.dto.RecebimentoRequest;
import br.com.emr.emrfinancas.dto.RecebimentoResponse;
import br.com.emr.emrfinancas.service.RecebimentoService;
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
@RequestMapping("/api/recebimentos")
public class RecebimentoController {
    private final RecebimentoService recebimentoService;

    public RecebimentoController(RecebimentoService recebimentoService) {
        this.recebimentoService = recebimentoService;
    }

    @GetMapping
    public ResponseEntity<List<RecebimentoResponse>> listar() {
        return ResponseEntity.ok(recebimentoService.listar());
    }

    @GetMapping("/{codigo}")
    public ResponseEntity<RecebimentoResponse> buscarPorId(@PathVariable Long codigo) {
        return ResponseEntity.ok(recebimentoService.buscarPorId(codigo));
    }

    @PostMapping
    public ResponseEntity<RecebimentoResponse> cadastrar(@Valid @RequestBody RecebimentoRequest recebimento) {
        return ResponseEntity.status(HttpStatus.CREATED).body(recebimentoService.cadastrar(recebimento));
    }

    @PutMapping("/{codigo}")
    public ResponseEntity<RecebimentoResponse> atualizar(@PathVariable Long codigo, @Valid @RequestBody RecebimentoRequest recebimento) {
        return ResponseEntity.ok(recebimentoService.atualizar(codigo, recebimento));
    }

    @DeleteMapping("/{codigo}")
    public ResponseEntity<Void> deletar(@PathVariable Long codigo) {
        recebimentoService.deletar(codigo);
        return ResponseEntity.noContent().build();
    }
}
