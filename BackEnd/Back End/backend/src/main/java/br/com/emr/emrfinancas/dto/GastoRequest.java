package br.com.emr.emrfinancas.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GastoRequest(
        @NotBlank(message = "A descricao do gasto e obrigatoria")
        @Size(min = 2, max = 120, message = "A descricao deve ter entre 2 e 120 caracteres")
        String descricao,
        @NotBlank(message = "A categoria do gasto e obrigatoria") String categoria,
        @NotNull(message = "O valor do gasto e obrigatorio")
        @Positive(message = "O valor do gasto deve ser maior que zero") BigDecimal valor,
        @NotNull(message = "A data do gasto e obrigatoria") LocalDate data,
        String formaPagamento,
        String observacao) {
}
