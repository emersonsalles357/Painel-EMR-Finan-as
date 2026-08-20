package br.com.emr.emrfinancas.controller;

import br.com.emr.emrfinancas.model.Investimento;
import br.com.emr.emrfinancas.service.InvestimentoService;
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
@RequestMapping("/api/investimentos")
public class InvestimentoController {
    private final InvestimentoService investimentoService;

    public InvestimentoController(InvestimentoService investimentoService) {
        this.investimentoService = investimentoService;
    }

    @GetMapping
    public ResponseEntity<List<Investimento>> listar() {
        return ResponseEntity.ok(investimentoService.listar());
    }

    @GetMapping("/{codigo}")
    public ResponseEntity<Investimento> buscarPorId(@PathVariable Long codigo) {
        return ResponseEntity.ok(investimentoService.buscarPorId(codigo));
    }

    @PostMapping
    public ResponseEntity<Investimento> cadastrar(@Valid @RequestBody Investimento investimento) {
        return ResponseEntity.status(HttpStatus.CREATED).body(investimentoService.cadastrar(investimento));
    }

    @PutMapping("/{codigo}")
    public ResponseEntity<Investimento> atualizar(@PathVariable Long codigo, @Valid @RequestBody Investimento investimento) {
        return ResponseEntity.ok(investimentoService.atualizar(codigo, investimento));
    }

    @DeleteMapping("/{codigo}")
    public ResponseEntity<Void> deletar(@PathVariable Long codigo) {
        investimentoService.deletar(codigo);
        return ResponseEntity.noContent().build();
    }
}
