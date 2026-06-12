package br.com.emr.emrfinancas.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "TB_EMR_INVESTIMENTO")
public class Investimento {
    @Id
    @Column(name = "CD_INVESTIMENTO")
    @SequenceGenerator(name = "seqInvestimento", sequenceName = "SEQ_EMR_INVESTIMENTO", allocationSize = 1)
    @GeneratedValue(generator = "seqInvestimento", strategy = GenerationType.SEQUENCE)
    private Long codigo;

    @NotBlank(message = "O nome do investimento e obrigatorio")
    @Size(min = 2, max = 120, message = "O nome deve ter entre 2 e 120 caracteres")
    @Column(name = "NM_INVESTIMENTO", nullable = false, length = 120)
    private String nome;

    @NotBlank(message = "O tipo do investimento e obrigatorio")
    @Column(name = "DS_TIPO", nullable = false, length = 80)
    private String tipo;

    @Column(name = "DS_INSTITUICAO", length = 100)
    private String instituicao;

    @NotNull(message = "O valor aplicado e obrigatorio")
    @Positive(message = "O valor aplicado deve ser maior que zero")
    @Column(name = "VL_APLICADO", nullable = false, precision = 12, scale = 2)
    private BigDecimal valorAplicado;

    @Column(name = "NR_RENTABILIDADE", precision = 8, scale = 2)
    private BigDecimal rentabilidadeMensal;

    @NotNull(message = "A data de aplicacao e obrigatoria")
    @Column(name = "DT_APLICACAO", nullable = false)
    private LocalDate dataAplicacao;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "CD_USUARIO")
    private Usuario usuario;

    public Long getCodigo() { return codigo; }
    public void setCodigo(Long codigo) { this.codigo = codigo; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public String getInstituicao() { return instituicao; }
    public void setInstituicao(String instituicao) { this.instituicao = instituicao; }
    public BigDecimal getValorAplicado() { return valorAplicado; }
    public void setValorAplicado(BigDecimal valorAplicado) { this.valorAplicado = valorAplicado; }
    public BigDecimal getRentabilidadeMensal() { return rentabilidadeMensal; }
    public void setRentabilidadeMensal(BigDecimal rentabilidadeMensal) { this.rentabilidadeMensal = rentabilidadeMensal; }
    public LocalDate getDataAplicacao() { return dataAplicacao; }
    public void setDataAplicacao(LocalDate dataAplicacao) { this.dataAplicacao = dataAplicacao; }
    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }
}
