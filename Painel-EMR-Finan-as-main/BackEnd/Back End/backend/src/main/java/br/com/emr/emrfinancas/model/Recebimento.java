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
@Table(name = "TB_EMR_RECEBIMENTO")
public class Recebimento {
    @Id
    @Column(name = "CD_RECEBIMENTO")
    @SequenceGenerator(name = "seqRecebimento", sequenceName = "SEQ_EMR_RECEBIMENTO", allocationSize = 1)
    @GeneratedValue(generator = "seqRecebimento", strategy = GenerationType.SEQUENCE)
    private Long codigo;

    @NotBlank(message = "A descricao do recebimento e obrigatoria")
    @Size(min = 2, max = 120, message = "A descricao deve ter entre 2 e 120 caracteres")
    @Column(name = "DS_DESCRICAO", nullable = false, length = 120)
    private String descricao;

    @NotBlank(message = "A origem do recebimento e obrigatoria")
    @Column(name = "DS_ORIGEM", nullable = false, length = 80)
    private String origem;

    @NotNull(message = "O valor do recebimento e obrigatorio")
    @Positive(message = "O valor do recebimento deve ser maior que zero")
    @Column(name = "VL_RECEBIMENTO", nullable = false, precision = 12, scale = 2)
    private BigDecimal valor;

    @NotNull(message = "A data do recebimento e obrigatoria")
    @Column(name = "DT_RECEBIMENTO", nullable = false)
    private LocalDate data;

    @NotBlank(message = "O status e obrigatorio")
    @Column(name = "DS_STATUS", nullable = false, length = 40)
    private String status;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "CD_USUARIO")
    private Usuario usuario;

    public Long getCodigo() { return codigo; }
    public void setCodigo(Long codigo) { this.codigo = codigo; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public String getOrigem() { return origem; }
    public void setOrigem(String origem) { this.origem = origem; }
    public BigDecimal getValor() { return valor; }
    public void setValor(BigDecimal valor) { this.valor = valor; }
    public LocalDate getData() { return data; }
    public void setData(LocalDate data) { this.data = data; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }
}
