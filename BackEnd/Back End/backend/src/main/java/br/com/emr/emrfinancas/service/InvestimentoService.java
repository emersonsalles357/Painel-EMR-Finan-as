package br.com.emr.emrfinancas.service;

import br.com.emr.emrfinancas.dto.InvestimentoRequest;
import br.com.emr.emrfinancas.dto.InvestimentoResponse;
import br.com.emr.emrfinancas.exception.RecursoNaoEncontradoException;
import br.com.emr.emrfinancas.model.Investimento;
import br.com.emr.emrfinancas.model.Usuario;
import br.com.emr.emrfinancas.repository.InvestimentoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class InvestimentoService {
    private final InvestimentoRepository investimentoRepository;
    private final AuthenticatedUserService authenticatedUserService;

    public InvestimentoService(InvestimentoRepository investimentoRepository,
                               AuthenticatedUserService authenticatedUserService) {
        this.investimentoRepository = investimentoRepository;
        this.authenticatedUserService = authenticatedUserService;
    }

    public List<InvestimentoResponse> listar() {
        Long usuarioId = authenticatedUserService.getAuthenticatedUser().getCodigo();
        return investimentoRepository.findAllByUsuarioCodigo(usuarioId).stream()
                .map(InvestimentoResponse::from).toList();
    }

    public InvestimentoResponse buscarPorId(Long codigo) {
        return InvestimentoResponse.from(buscarDoUsuario(codigo));
    }

    @Transactional
    public InvestimentoResponse cadastrar(InvestimentoRequest request) {
        Usuario usuario = authenticatedUserService.getAuthenticatedUser();
        Investimento investimento = new Investimento();
        aplicar(request, investimento);
        investimento.setUsuario(usuario);
        return InvestimentoResponse.from(investimentoRepository.save(investimento));
    }

    @Transactional
    public InvestimentoResponse atualizar(Long codigo, InvestimentoRequest request) {
        Investimento investimento = buscarDoUsuario(codigo);
        aplicar(request, investimento);
        return InvestimentoResponse.from(investimentoRepository.save(investimento));
    }

    @Transactional
    public void deletar(Long codigo) {
        investimentoRepository.delete(buscarDoUsuario(codigo));
    }

    private Investimento buscarDoUsuario(Long codigo) {
        Long usuarioId = authenticatedUserService.getAuthenticatedUser().getCodigo();
        return investimentoRepository.findByCodigoAndUsuarioCodigo(codigo, usuarioId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Recurso nao encontrado"));
    }

    private void aplicar(InvestimentoRequest request, Investimento investimento) {
        investimento.setNome(request.nome());
        investimento.setTipo(request.tipo());
        investimento.setInstituicao(request.instituicao());
        investimento.setValorAplicado(request.valorAplicado());
        investimento.setRentabilidadeMensal(request.rentabilidadeMensal());
        investimento.setDataAplicacao(request.dataAplicacao());
    }
}
