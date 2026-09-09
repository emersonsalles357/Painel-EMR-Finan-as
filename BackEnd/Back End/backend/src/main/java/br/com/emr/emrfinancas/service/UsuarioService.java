package br.com.emr.emrfinancas.service;

import br.com.emr.emrfinancas.dto.UsuarioResponse;
import br.com.emr.emrfinancas.exception.RecursoNaoEncontradoException;
import br.com.emr.emrfinancas.exception.RegraNegocioException;
import br.com.emr.emrfinancas.model.Usuario;
import br.com.emr.emrfinancas.model.UserRole;
import br.com.emr.emrfinancas.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

@Service
public class UsuarioService {
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailNormalizer emailNormalizer;

    public UsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder,
                          EmailNormalizer emailNormalizer) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailNormalizer = emailNormalizer;
    }

    public List<UsuarioResponse> listar() {
        return usuarioRepository.findAll().stream().map(UsuarioResponse::from).toList();
    }

    public UsuarioResponse buscarPorId(Long codigo) {
        return UsuarioResponse.from(buscarEntidade(codigo));
    }

    private Usuario buscarEntidade(Long codigo) {
        return usuarioRepository.findById(codigo)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuario nao encontrado"));
    }

    public UsuarioResponse cadastrar(Usuario usuario) {
        usuario.setNome(usuario.getNome().trim());
        usuario.setEmail(emailNormalizer.normalize(usuario.getEmail()));
        usuarioRepository.findByEmailIgnoreCase(usuario.getEmail()).ifPresent(u -> {
            throw new RegraNegocioException("Ja existe usuario cadastrado com este e-mail");
        });
        usuario.setSenha(passwordEncoder.encode(usuario.getSenha()));
        usuario.setRole(UserRole.USER);
        return UsuarioResponse.from(usuarioRepository.save(usuario));
    }

    public UsuarioResponse atualizar(Long codigo, Usuario usuarioAtualizado) {
        Usuario usuario = buscarEntidade(codigo);
        usuario.setNome(usuarioAtualizado.getNome().trim());
        usuario.setEmail(emailNormalizer.normalize(usuarioAtualizado.getEmail()));
        usuario.setSenha(passwordEncoder.encode(usuarioAtualizado.getSenha()));
        usuario.invalidateTokens();
        return UsuarioResponse.from(usuarioRepository.save(usuario));
    }

    public void deletar(Long codigo) {
        Usuario usuario = buscarEntidade(codigo);
        usuarioRepository.delete(usuario);
    }
}
