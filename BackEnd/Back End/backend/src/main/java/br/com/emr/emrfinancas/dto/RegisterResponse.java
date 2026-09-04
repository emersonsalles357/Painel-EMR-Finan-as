package br.com.emr.emrfinancas.dto;

import br.com.emr.emrfinancas.model.Usuario;

public record RegisterResponse(Long id, String nome, String email, String mensagem) {
    public static RegisterResponse from(Usuario usuario) {
        return new RegisterResponse(usuario.getCodigo(), usuario.getNome(), usuario.getEmail(),
                "Conta criada com sucesso");
    }
}
