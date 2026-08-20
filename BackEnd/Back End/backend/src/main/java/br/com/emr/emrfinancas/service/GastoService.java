package br.com.emr.emrfinancas.service;

import br.com.emr.emrfinancas.exception.RecursoNaoEncontradoException;
import br.com.emr.emrfinancas.model.Gasto;
import br.com.emr.emrfinancas.model.Usuario;
import br.com.emr.emrfinancas.repository.GastoRepository;
import br.com.emr.emrfinancas.repository.UsuarioRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class GastoService {
    private final GastoRepository gastoRepository;
    private final UsuarioRepository usuarioRepository;

    public GastoService(GastoRepository gastoRepository, UsuarioRepository usuarioRepository) {
        this.gastoRepository = gastoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    public List<Gasto> listar() { return gastoRepository.findAll(); }

    public Gasto buscarPorId(Long codigo) {
        return gastoRepository.findById(codigo)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Gasto nao encontrado"));
    }

    public Gasto cadastrar(Gasto gasto) {
        preparar(gasto);
        return gastoRepository.save(gasto);
    }

    public Gasto atualizar(Long codigo, Gasto gastoAtualizado) {
        Gasto gasto = buscarPorId(codigo);
        gasto.setDescricao(gastoAtualizado.getDescricao());
        gasto.setCategoria(gastoAtualizado.getCategoria());
        gasto.setValor(gastoAtualizado.getValor());
        gasto.setData(gastoAtualizado.getData());
        gasto.setFormaPagamento(gastoAtualizado.getFormaPagamento());
        gasto.setObservacao(gastoAtualizado.getObservacao());
        gasto.setUsuario(gastoAtualizado.getUsuario());
        preparar(gasto);
        return gastoRepository.save(gasto);
    }

    public void deletar(Long codigo) {
        Gasto gasto = buscarPorId(codigo);
        gastoRepository.delete(gasto);
    }

    private void preparar(Gasto gasto) {
        if (gasto.getData() == null) gasto.setData(LocalDate.now());
        if (gasto.getUsuario() == null || gasto.getUsuario().getCodigo() == null) {
            Usuario usuario = usuarioRepository.findAll().stream().findFirst()
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Cadastre um usuario antes de cadastrar gastos"));
            gasto.setUsuario(usuario);
        } else {
            Usuario usuario = usuarioRepository.findById(gasto.getUsuario().getCodigo())
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Usuario informado nao encontrado"));
            gasto.setUsuario(usuario);
        }
    }
}
