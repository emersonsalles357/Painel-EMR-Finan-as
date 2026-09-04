package br.com.emr.emrfinancas.dto;

public class LoginResponse {
    private Long codigo;
    private String nome;
    private String email;
    private String token;

    public LoginResponse(Long codigo, String nome, String email, String token) {
        this.codigo = codigo;
        this.nome = nome;
        this.email = email;
        this.token = token;
    }

    public Long getCodigo() { return codigo; }
    public String getNome() { return nome; }
    public String getEmail() { return email; }
    public String getToken() { return token; }
}
