package br.com.emr.emrfinancas.service;

import br.com.emr.emrfinancas.dto.DashboardResponse;
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
    private final AuthenticatedUserService authenticatedUserService;

    public DashboardService(GastoRepository gastoRepository, RecebimentoRepository recebimentoRepository,
                            InvestimentoRepository investimentoRepository,
                            AuthenticatedUserService authenticatedUserService) {
        this.gastoRepository = gastoRepository;
        this.recebimentoRepository = recebimentoRepository;
        this.investimentoRepository = investimentoRepository;
        this.authenticatedUserService = authenticatedUserService;
    }

    public DashboardResponse consolidar() {
        Long usuarioId = authenticatedUserService.getAuthenticatedUser().getCodigo();
        BigDecimal gastos = gastoRepository.somarPorUsuario(usuarioId);
        BigDecimal recebimentos = recebimentoRepository.somarPorUsuario(usuarioId);
        BigDecimal investimentos = investimentoRepository.somarPorUsuario(usuarioId);
        BigDecimal saldo = recebimentos.subtract(gastos).add(investimentos);
        return new DashboardResponse(gastos, recebimentos, investimentos, saldo);
    }
}
