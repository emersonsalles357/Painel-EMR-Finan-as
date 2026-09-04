package br.com.emr.emrfinancas.dto;

import br.com.emr.emrfinancas.model.Investimento;

import java.math.BigDecimal;
import java.time.LocalDate;

public record InvestimentoResponse(Long codigo, String nome, String tipo, String instituicao,
                                   BigDecimal valorAplicado, BigDecimal rentabilidadeMensal,
                                   LocalDate dataAplicacao) {
    public static InvestimentoResponse from(Investimento investimento) {
        return new InvestimentoResponse(investimento.getCodigo(), investimento.getNome(), investimento.getTipo(),
                investimento.getInstituicao(), investimento.getValorAplicado(),
                investimento.getRentabilidadeMensal(), investimento.getDataAplicacao());
    }
}
