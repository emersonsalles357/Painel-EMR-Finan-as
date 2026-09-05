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

import java.time.Instant;

@Entity
@Table(name = "TB_EMR_PASSWORD_RESET_TOKEN")
public class PasswordResetToken {
    @Id
    @Column(name = "CD_TOKEN")
    @SequenceGenerator(name = "seqPasswordResetToken", sequenceName = "SEQ_EMR_PASSWORD_RESET_TOKEN", allocationSize = 1)
    @GeneratedValue(generator = "seqPasswordResetToken", strategy = GenerationType.SEQUENCE)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "CD_USUARIO", nullable = false)
    private Usuario usuario;

    @Column(name = "DS_TOKEN_HASH", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(name = "DT_CRIACAO", nullable = false)
    private Instant createdAt;

    @Column(name = "DT_EXPIRACAO", nullable = false)
    private Instant expiresAt;

    @Column(name = "DT_UTILIZACAO")
    private Instant usedAt;

    public PasswordResetToken() {
    }

    public PasswordResetToken(Usuario usuario, String tokenHash, Instant createdAt, Instant expiresAt) {
        this.usuario = usuario;
        this.tokenHash = tokenHash;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }

    public String getTokenHash() { return tokenHash; }
    public void setTokenHash(String tokenHash) { this.tokenHash = tokenHash; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }

    public Instant getUsedAt() { return usedAt; }
    public void setUsedAt(Instant usedAt) { this.usedAt = usedAt; }

    public boolean isUsed() {
        return usedAt != null;
    }

    public boolean isExpired(Instant now) {
        return expiresAt.isBefore(now);
    }

    public boolean isValid(Instant now) {
        return !isUsed() && !isExpired(now);
    }
}
