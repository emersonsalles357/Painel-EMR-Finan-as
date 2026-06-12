package br.com.emr.emrfinancas.controller;

import br.com.emr.emrfinancas.model.Gasto;
import br.com.emr.emrfinancas.service.GastoService;
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
@RequestMapping("/api/gastos")
public class GastoController {
    private final GastoService gastoService;

    public GastoController(GastoService gastoService) {
        this.gastoService = gastoService;
    }

    @GetMapping
    public ResponseEntity<List<Gasto>> listar() {
        return ResponseEntity.ok(gastoService.listar());
    }

    @GetMapping("/{codigo}")
    public ResponseEntity<Gasto> buscarPorId(@PathVariable Long codigo) {
        return ResponseEntity.ok(gastoService.buscarPorId(codigo));
    }

    @PostMapping
    public ResponseEntity<Gasto> cadastrar(@Valid @RequestBody Gasto gasto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(gastoService.cadastrar(gasto));
    }

    @PutMapping("/{codigo}")
    public ResponseEntity<Gasto> atualizar(@PathVariable Long codigo, @Valid @RequestBody Gasto gasto) {
        return ResponseEntity.ok(gastoService.atualizar(codigo, gasto));
    }

    @DeleteMapping("/{codigo}")
    public ResponseEntity<Void> deletar(@PathVariable Long codigo) {
        gastoService.deletar(codigo);
        return ResponseEntity.noContent().build();
    }
}
