package br.com.emr.emrfinancas.service;

import br.com.emr.emrfinancas.exception.RecursoNaoEncontradoException;
import br.com.emr.emrfinancas.exception.RegraNegocioException;
import br.com.emr.emrfinancas.model.Usuario;
import br.com.emr.emrfinancas.repository.UsuarioRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UsuarioService {
    private final UsuarioRepository usuarioRepository;

    public UsuarioService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    public List<Usuario> listar() { return usuarioRepository.findAll(); }

    public Usuario buscarPorId(Long codigo) {
        return usuarioRepository.findById(codigo)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuario nao encontrado"));
    }

    public Usuario cadastrar(Usuario usuario) {
        usuarioRepository.findByEmail(usuario.getEmail()).ifPresent(u -> {
            throw new RegraNegocioException("Ja existe usuario cadastrado com este e-mail");
        });
        return usuarioRepository.save(usuario);
    }

    public Usuario atualizar(Long codigo, Usuario usuarioAtualizado) {
        Usuario usuario = buscarPorId(codigo);
        usuario.setNome(usuarioAtualizado.getNome());
        usuario.setEmail(usuarioAtualizado.getEmail());
        usuario.setSenha(usuarioAtualizado.getSenha());
        return usuarioRepository.save(usuario);
    }

    public void deletar(Long codigo) {
        Usuario usuario = buscarPorId(codigo);
        usuarioRepository.delete(usuario);
    }
}
