package br.com.emr.emrfinancas.service;

import br.com.emr.emrfinancas.exception.RecursoNaoEncontradoException;
import br.com.emr.emrfinancas.model.Recebimento;
import br.com.emr.emrfinancas.model.Usuario;
import br.com.emr.emrfinancas.repository.RecebimentoRepository;
import br.com.emr.emrfinancas.repository.UsuarioRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class RecebimentoService {
    private final RecebimentoRepository recebimentoRepository;
    private final UsuarioRepository usuarioRepository;

    public RecebimentoService(RecebimentoRepository recebimentoRepository, UsuarioRepository usuarioRepository) {
        this.recebimentoRepository = recebimentoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    public List<Recebimento> listar() { return recebimentoRepository.findAll(); }

    public Recebimento buscarPorId(Long codigo) {
        return recebimentoRepository.findById(codigo)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Recebimento nao encontrado"));
    }

    public Recebimento cadastrar(Recebimento recebimento) {
        preparar(recebimento);
        return recebimentoRepository.save(recebimento);
    }

    public Recebimento atualizar(Long codigo, Recebimento recebimentoAtualizado) {
        Recebimento recebimento = buscarPorId(codigo);
        recebimento.setDescricao(recebimentoAtualizado.getDescricao());
        recebimento.setOrigem(recebimentoAtualizado.getOrigem());
        recebimento.setValor(recebimentoAtualizado.getValor());
        recebimento.setData(recebimentoAtualizado.getData());
        recebimento.setStatus(recebimentoAtualizado.getStatus());
        recebimento.setUsuario(recebimentoAtualizado.getUsuario());
        preparar(recebimento);
        return recebimentoRepository.save(recebimento);
    }

    public void deletar(Long codigo) {
        Recebimento recebimento = buscarPorId(codigo);
        recebimentoRepository.delete(recebimento);
    }

    private void preparar(Recebimento recebimento) {
        if (recebimento.getData() == null) recebimento.setData(LocalDate.now());
        if (recebimento.getStatus() == null || recebimento.getStatus().isBlank()) recebimento.setStatus("Recebido");
        if (recebimento.getUsuario() == null || recebimento.getUsuario().getCodigo() == null) {
            Usuario usuario = usuarioRepository.findAll().stream().findFirst()
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Cadastre um usuario antes de cadastrar recebimentos"));
            recebimento.setUsuario(usuario);
        } else {
            Usuario usuario = usuarioRepository.findById(recebimento.getUsuario().getCodigo())
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Usuario informado nao encontrado"));
            recebimento.setUsuario(usuario);
        }
    }
}
