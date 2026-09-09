package br.com.emr.emrfinancas.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

@JsonIgnoreProperties(ignoreUnknown = true)
public record RecebimentoRequest(
        @NotBlank(message = "A descricao do recebimento e obrigatoria")
        @Size(min = 2, max = 120, message = "A descricao deve ter entre 2 e 120 caracteres")
        String descricao,
        @NotBlank(message = "A origem do recebimento e obrigatoria") String origem,
        @NotNull(message = "O valor do recebimento e obrigatorio")
        @Positive(message = "O valor do recebimento deve ser maior que zero") BigDecimal valor,
        @NotNull(message = "A data do recebimento e obrigatoria") LocalDate data,
        @NotBlank(message = "O status e obrigatorio") String status) {
}
