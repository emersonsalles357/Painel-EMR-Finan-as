package br.com.emr.emrfinancas.service;

import br.com.emr.emrfinancas.exception.RecursoNaoEncontradoException;
import br.com.emr.emrfinancas.model.Investimento;
import br.com.emr.emrfinancas.model.Usuario;
import br.com.emr.emrfinancas.repository.InvestimentoRepository;
import br.com.emr.emrfinancas.repository.UsuarioRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class InvestimentoService {
    private final InvestimentoRepository investimentoRepository;
    private final UsuarioRepository usuarioRepository;

    public InvestimentoService(InvestimentoRepository investimentoRepository, UsuarioRepository usuarioRepository) {
        this.investimentoRepository = investimentoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    public List<Investimento> listar() { return investimentoRepository.findAll(); }

    public Investimento buscarPorId(Long codigo) {
        return investimentoRepository.findById(codigo)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Investimento nao encontrado"));
    }

    public Investimento cadastrar(Investimento investimento) {
        preparar(investimento);
        return investimentoRepository.save(investimento);
    }

    public Investimento atualizar(Long codigo, Investimento investimentoAtualizado) {
        Investimento investimento = buscarPorId(codigo);
        investimento.setNome(investimentoAtualizado.getNome());
        investimento.setTipo(investimentoAtualizado.getTipo());
        investimento.setInstituicao(investimentoAtualizado.getInstituicao());
        investimento.setValorAplicado(investimentoAtualizado.getValorAplicado());
        investimento.setRentabilidadeMensal(investimentoAtualizado.getRentabilidadeMensal());
        investimento.setDataAplicacao(investimentoAtualizado.getDataAplicacao());
        investimento.setUsuario(investimentoAtualizado.getUsuario());
        preparar(investimento);
        return investimentoRepository.save(investimento);
    }

    public void deletar(Long codigo) {
        Investimento investimento = buscarPorId(codigo);
        investimentoRepository.delete(investimento);
    }

    private void preparar(Investimento investimento) {
        if (investimento.getDataAplicacao() == null) investimento.setDataAplicacao(LocalDate.now());
        if (investimento.getUsuario() == null || investimento.getUsuario().getCodigo() == null) {
            Usuario usuario = usuarioRepository.findAll().stream().findFirst()
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Cadastre um usuario antes de cadastrar investimentos"));
            investimento.setUsuario(usuario);
        } else {
            Usuario usuario = usuarioRepository.findById(investimento.getUsuario().getCodigo())
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Usuario informado nao encontrado"));
            investimento.setUsuario(usuario);
        }
    }
}
