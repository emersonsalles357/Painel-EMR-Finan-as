package br.com.emr.emrfinancas.service;

import br.com.emr.emrfinancas.dto.DashboardResponse;
import br.com.emr.emrfinancas.model.Gasto;
import br.com.emr.emrfinancas.model.Investimento;
import br.com.emr.emrfinancas.model.Recebimento;
import br.com.emr.emrfinancas.repository.GastoRepository;
import br.com.emr.emrfinancas.repository.InvestimentoRepository;
import br.com.emr.emrfinancas.repository.RecebimentoRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class DashboardService {
    private final GastoRepository gastoRepository;
    private final RecebimentoRepository recebimentoRepository;
    private final InvestimentoRepository investimentoRepository;

    public DashboardService(GastoRepository gastoRepository, RecebimentoRepository recebimentoRepository, InvestimentoRepository investimentoRepository) {
        this.gastoRepository = gastoRepository;
        this.recebimentoRepository = recebimentoRepository;
        this.investimentoRepository = investimentoRepository;
    }

    public DashboardResponse consolidar() {
        BigDecimal gastos = gastoRepository.findAll().stream().map(Gasto::getValor).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal recebimentos = recebimentoRepository.findAll().stream().map(Recebimento::getValor).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal investimentos = investimentoRepository.findAll().stream().map(Investimento::getValorAplicado).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal saldo = recebimentos.subtract(gastos).add(investimentos);
        return new DashboardResponse(gastos, recebimentos, investimentos, saldo);
    }
}
