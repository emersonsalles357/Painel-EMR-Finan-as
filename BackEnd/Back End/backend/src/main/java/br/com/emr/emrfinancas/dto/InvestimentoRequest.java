package br.com.emr.emrfinancas.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

@JsonIgnoreProperties(ignoreUnknown = true)
public record InvestimentoRequest(
        @NotBlank(message = "O nome do investimento e obrigatorio")
        @Size(min = 2, max = 120, message = "O nome deve ter entre 2 e 120 caracteres") String nome,
        @NotBlank(message = "O tipo do investimento e obrigatorio") String tipo,
        String instituicao,
        @NotNull(message = "O valor aplicado e obrigatorio")
        @Positive(message = "O valor aplicado deve ser maior que zero") BigDecimal valorAplicado,
        BigDecimal rentabilidadeMensal,
        @NotNull(message = "A data de aplicacao e obrigatoria") LocalDate dataAplicacao) {
}
