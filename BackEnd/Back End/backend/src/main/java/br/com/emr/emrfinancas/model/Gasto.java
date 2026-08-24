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
@Table(name = "TB_EMR_GASTO")
public class Gasto {
    @Id
    @Column(name = "CD_GASTO")
    @SequenceGenerator(name = "seqGasto", sequenceName = "SEQ_EMR_GASTO", allocationSize = 1)
    @GeneratedValue(generator = "seqGasto", strategy = GenerationType.SEQUENCE)
    private Long codigo;

    @NotBlank(message = "A descricao do gasto e obrigatoria")
    @Size(min = 2, max = 120, message = "A descricao deve ter entre 2 e 120 caracteres")
    @Column(name = "DS_DESCRICAO", nullable = false, length = 120)
    private String descricao;

    @NotBlank(message = "A categoria do gasto e obrigatoria")
    @Column(name = "DS_CATEGORIA", nullable = false, length = 80)
    private String categoria;

    @NotNull(message = "O valor do gasto e obrigatorio")
    @Positive(message = "O valor do gasto deve ser maior que zero")
    @Column(name = "VL_GASTO", nullable = false, precision = 12, scale = 2)
    private BigDecimal valor;

    @NotNull(message = "A data do gasto e obrigatoria")
    @Column(name = "DT_GASTO", nullable = false)
    private LocalDate data;

    @Column(name = "DS_FORMA_PAGAMENTO", length = 60)
    private String formaPagamento;

    @Column(name = "DS_OBSERVACAO", length = 255)
    private String observacao;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "CD_USUARIO")
    private Usuario usuario;

    public Long getCodigo() { return codigo; }
    public void setCodigo(Long codigo) { this.codigo = codigo; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }
    public BigDecimal getValor() { return valor; }
    public void setValor(BigDecimal valor) { this.valor = valor; }
    public LocalDate getData() { return data; }
    public void setData(LocalDate data) { this.data = data; }
    public String getFormaPagamento() { return formaPagamento; }
    public void setFormaPagamento(String formaPagamento) { this.formaPagamento = formaPagamento; }
    public String getObservacao() { return observacao; }
    public void setObservacao(String observacao) { this.observacao = observacao; }
    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }
}
