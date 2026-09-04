package br.com.emr.emrfinancas.dto;

import java.math.BigDecimal;

public class DashboardResponse {
    private BigDecimal totalGastos;
    private BigDecimal totalRecebimentos;
    private BigDecimal totalInvestimentos;
    private BigDecimal saldo;

    public DashboardResponse(BigDecimal totalGastos, BigDecimal totalRecebimentos, BigDecimal totalInvestimentos, BigDecimal saldo) {
        this.totalGastos = totalGastos;
        this.totalRecebimentos = totalRecebimentos;
        this.totalInvestimentos = totalInvestimentos;
        this.saldo = saldo;
    }

    public BigDecimal getTotalGastos() { return totalGastos; }
    public BigDecimal getTotalRecebimentos() { return totalRecebimentos; }
    public BigDecimal getTotalInvestimentos() { return totalInvestimentos; }
    public BigDecimal getSaldo() { return saldo; }
}
