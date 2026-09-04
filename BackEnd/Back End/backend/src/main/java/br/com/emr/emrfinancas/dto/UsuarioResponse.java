package br.com.emr.emrfinancas.dto;

import br.com.emr.emrfinancas.model.Usuario;

public record UsuarioResponse(Long id, String nome, String email, String role) {
    public static UsuarioResponse from(Usuario usuario) {
        return new UsuarioResponse(usuario.getCodigo(), usuario.getNome(), usuario.getEmail(), "ROLE_USER");
    }
}
