package br.com.emr.emrfinancas.dto;

import br.com.emr.emrfinancas.model.Gasto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record GastoResponse(Long codigo, String descricao, String categoria, BigDecimal valor,
                            LocalDate data, String formaPagamento, String observacao) {
    public static GastoResponse from(Gasto gasto) {
        return new GastoResponse(gasto.getCodigo(), gasto.getDescricao(), gasto.getCategoria(), gasto.getValor(),
                gasto.getData(), gasto.getFormaPagamento(), gasto.getObservacao());
    }
}
