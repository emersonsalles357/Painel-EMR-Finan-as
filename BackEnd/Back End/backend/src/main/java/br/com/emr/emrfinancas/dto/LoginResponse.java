package br.com.emr.emrfinancas.dto;

public class LoginResponse {
    private final String accessToken;
    private final String tokenType;
    private final long expiresIn;
    private final UsuarioResponse usuario;

    public LoginResponse(String accessToken, long expiresIn, UsuarioResponse usuario) {
        this.accessToken = accessToken;
        this.tokenType = "Bearer";
        this.expiresIn = expiresIn;
        this.usuario = usuario;
    }

    public String getAccessToken() { return accessToken; }
    public String getTokenType() { return tokenType; }
    public long getExpiresIn() { return expiresIn; }
    public UsuarioResponse getUsuario() { return usuario; }
}
