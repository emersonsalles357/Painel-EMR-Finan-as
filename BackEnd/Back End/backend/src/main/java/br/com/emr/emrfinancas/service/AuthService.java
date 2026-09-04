package br.com.emr.emrfinancas.service;

import br.com.emr.emrfinancas.dto.LoginRequest;
import br.com.emr.emrfinancas.dto.LoginResponse;
import br.com.emr.emrfinancas.exception.RegraNegocioException;
import br.com.emr.emrfinancas.model.Usuario;
import br.com.emr.emrfinancas.repository.UsuarioRepository;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    private final UsuarioRepository usuarioRepository;

    public AuthService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    public LoginResponse login(LoginRequest loginRequest) {
        Usuario usuario = usuarioRepository.findByEmailAndSenha(loginRequest.getEmail(), loginRequest.getSenha())
                .orElseThrow(() -> new RegraNegocioException("E-mail ou senha invalidos"));
        return new LoginResponse(usuario.getCodigo(), usuario.getNome(), usuario.getEmail(), "token-academico-emr");
    }
}
