package br.com.emr.emrfinancas.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "TB_EMR_USUARIO")
public class Usuario {
    @Id
    @Column(name = "CD_USUARIO")
    @SequenceGenerator(name = "seqUsuario", sequenceName = "SEQ_EMR_USUARIO", allocationSize = 1)
    @GeneratedValue(generator = "seqUsuario", strategy = GenerationType.SEQUENCE)
    private Long codigo;

    @NotBlank(message = "O nome do usuario e obrigatorio")
    @Size(min = 2, max = 100, message = "O nome deve ter entre 2 e 100 caracteres")
    @Column(name = "NM_USUARIO", nullable = false, length = 100)
    private String nome;

    @NotBlank(message = "O e-mail e obrigatorio")
    @Email(message = "Informe um e-mail valido")
    @Column(name = "DS_EMAIL", nullable = false, unique = true, length = 120)
    private String email;

    @NotBlank(message = "A senha e obrigatoria")
    @Size(min = 4, max = 100, message = "A senha deve ter pelo menos 4 caracteres")
    @Column(name = "DS_SENHA", nullable = false, length = 100)
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String senha;

    @Enumerated(EnumType.STRING)
    @Column(name = "DS_ROLE", nullable = false, length = 20)
    private UserRole role = UserRole.USER;

    @Column(name = "NR_FALHAS_LOGIN", nullable = false)
    private int failedLoginAttempts = 0;

    @Column(name = "DT_BLOQUEIO_ATE")
    private java.time.Instant lockedUntil;

    @Column(name = "NR_TOKEN_VERSION", nullable = false)
    private long tokenVersion = 0;

    public Long getCodigo() { return codigo; }
    public void setCodigo(Long codigo) { this.codigo = codigo; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getSenha() { return senha; }
    public void setSenha(String senha) { this.senha = senha; }
    public UserRole getRole() { return role; }
    public void setRole(UserRole role) { this.role = role; }

    public int getFailedLoginAttempts() { return failedLoginAttempts; }
    public void setFailedLoginAttempts(int failedLoginAttempts) { this.failedLoginAttempts = failedLoginAttempts; }
    public java.time.Instant getLockedUntil() { return lockedUntil; }
    public void setLockedUntil(java.time.Instant lockedUntil) { this.lockedUntil = lockedUntil; }
    public long getTokenVersion() { return tokenVersion; }

    public void invalidateTokens() {
        this.tokenVersion++;
    }

    public boolean isAccountLocked(java.time.Instant now) {
        return lockedUntil != null && lockedUntil.isAfter(now);
    }

    public void registerFailedLoginAttempt(java.time.Instant now, int maxAttempts, int lockoutMinutes) {
        this.failedLoginAttempts++;
        if (this.failedLoginAttempts >= maxAttempts) {
            this.lockedUntil = now.plus(lockoutMinutes, java.time.temporal.ChronoUnit.MINUTES);
        }
    }

    public void resetLockout() {
        this.failedLoginAttempts = 0;
        this.lockedUntil = null;
    }
}
