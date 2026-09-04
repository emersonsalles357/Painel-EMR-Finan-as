package br.com.emr.emrfinancas.service;

import br.com.emr.emrfinancas.dto.GastoRequest;
import br.com.emr.emrfinancas.dto.GastoResponse;
import br.com.emr.emrfinancas.exception.RecursoNaoEncontradoException;
import br.com.emr.emrfinancas.model.Gasto;
import br.com.emr.emrfinancas.model.Usuario;
import br.com.emr.emrfinancas.repository.GastoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class GastoService {
    private final GastoRepository gastoRepository;
    private final AuthenticatedUserService authenticatedUserService;

    public GastoService(GastoRepository gastoRepository, AuthenticatedUserService authenticatedUserService) {
        this.gastoRepository = gastoRepository;
        this.authenticatedUserService = authenticatedUserService;
    }

    public List<GastoResponse> listar() {
        Long usuarioId = authenticatedUserService.getAuthenticatedUser().getCodigo();
        return gastoRepository.findAllByUsuarioCodigo(usuarioId).stream().map(GastoResponse::from).toList();
    }

    public GastoResponse buscarPorId(Long codigo) {
        return GastoResponse.from(buscarDoUsuario(codigo));
    }

    @Transactional
    public GastoResponse cadastrar(GastoRequest request) {
        Usuario usuario = authenticatedUserService.getAuthenticatedUser();
        Gasto gasto = new Gasto();
        aplicar(request, gasto);
        gasto.setUsuario(usuario);
        return GastoResponse.from(gastoRepository.save(gasto));
    }

    @Transactional
    public GastoResponse atualizar(Long codigo, GastoRequest request) {
        Gasto gasto = buscarDoUsuario(codigo);
        aplicar(request, gasto);
        return GastoResponse.from(gastoRepository.save(gasto));
    }

    @Transactional
    public void deletar(Long codigo) {
        gastoRepository.delete(buscarDoUsuario(codigo));
    }

    private Gasto buscarDoUsuario(Long codigo) {
        Long usuarioId = authenticatedUserService.getAuthenticatedUser().getCodigo();
        return gastoRepository.findByCodigoAndUsuarioCodigo(codigo, usuarioId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Recurso nao encontrado"));
    }

    private void aplicar(GastoRequest request, Gasto gasto) {
        gasto.setDescricao(request.descricao());
        gasto.setCategoria(request.categoria());
        gasto.setValor(request.valor());
        gasto.setData(request.data());
        gasto.setFormaPagamento(request.formaPagamento());
        gasto.setObservacao(request.observacao());
    }
}
