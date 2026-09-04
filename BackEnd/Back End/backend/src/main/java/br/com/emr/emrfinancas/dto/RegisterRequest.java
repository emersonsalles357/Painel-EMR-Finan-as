package br.com.emr.emrfinancas.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@JsonIgnoreProperties(ignoreUnknown = true)
public class RegisterRequest {
    @NotBlank(message = "O nome e obrigatorio")
    @Size(min = 2, max = 100, message = "O nome deve ter entre 2 e 100 caracteres")
    private String nome;

    @NotBlank(message = "O e-mail e obrigatorio")
    @Email(message = "Informe um e-mail valido")
    @Size(max = 120, message = "O e-mail deve ter no maximo 120 caracteres")
    private String email;

    @NotBlank(message = "A senha e obrigatoria")
    @Size(max = 72, message = "A senha deve ter no maximo 72 caracteres")
    private String senha;

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome == null ? null : nome.trim(); }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email == null ? null : email.trim(); }
    public String getSenha() { return senha; }
    public void setSenha(String senha) { this.senha = senha; }
}
