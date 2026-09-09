package br.com.emr.emrfinancas.service;

import br.com.emr.emrfinancas.dto.RecebimentoRequest;
import br.com.emr.emrfinancas.dto.RecebimentoResponse;
import br.com.emr.emrfinancas.exception.RecursoNaoEncontradoException;
import br.com.emr.emrfinancas.model.Recebimento;
import br.com.emr.emrfinancas.model.Usuario;
import br.com.emr.emrfinancas.repository.RecebimentoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class RecebimentoService {
    private final RecebimentoRepository recebimentoRepository;
    private final AuthenticatedUserService authenticatedUserService;

    public RecebimentoService(RecebimentoRepository recebimentoRepository,
                              AuthenticatedUserService authenticatedUserService) {
        this.recebimentoRepository = recebimentoRepository;
        this.authenticatedUserService = authenticatedUserService;
    }

    public List<RecebimentoResponse> listar() {
        Long usuarioId = authenticatedUserService.getAuthenticatedUser().getCodigo();
        return recebimentoRepository.findAllByUsuarioCodigo(usuarioId).stream()
                .map(RecebimentoResponse::from).toList();
    }

    public RecebimentoResponse buscarPorId(Long codigo) {
        return RecebimentoResponse.from(buscarDoUsuario(codigo));
    }

    @Transactional
    public RecebimentoResponse cadastrar(RecebimentoRequest request) {
        Usuario usuario = authenticatedUserService.getAuthenticatedUser();
        Recebimento recebimento = new Recebimento();
        aplicar(request, recebimento);
        recebimento.setUsuario(usuario);
        return RecebimentoResponse.from(recebimentoRepository.save(recebimento));
    }

    @Transactional
    public RecebimentoResponse atualizar(Long codigo, RecebimentoRequest request) {
        Recebimento recebimento = buscarDoUsuario(codigo);
        aplicar(request, recebimento);
        return RecebimentoResponse.from(recebimentoRepository.save(recebimento));
    }

    @Transactional
    public void deletar(Long codigo) {
        recebimentoRepository.delete(buscarDoUsuario(codigo));
    }

    private Recebimento buscarDoUsuario(Long codigo) {
        Long usuarioId = authenticatedUserService.getAuthenticatedUser().getCodigo();
        return recebimentoRepository.findByCodigoAndUsuarioCodigo(codigo, usuarioId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Recurso nao encontrado"));
    }

    private void aplicar(RecebimentoRequest request, Recebimento recebimento) {
        recebimento.setDescricao(request.descricao());
        recebimento.setOrigem(request.origem());
        recebimento.setValor(request.valor());
        recebimento.setData(request.data());
        recebimento.setStatus(request.status());
    }
}
