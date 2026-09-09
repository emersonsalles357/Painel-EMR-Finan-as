package br.com.emr.emrfinancas.dto;

import br.com.emr.emrfinancas.model.Recebimento;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RecebimentoResponse(Long codigo, String descricao, String origem, BigDecimal valor,
                                  LocalDate data, String status) {
    public static RecebimentoResponse from(Recebimento recebimento) {
        return new RecebimentoResponse(recebimento.getCodigo(), recebimento.getDescricao(), recebimento.getOrigem(),
                recebimento.getValor(), recebimento.getData(), recebimento.getStatus());
    }
}
