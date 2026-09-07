package br.com.emr.emrfinancas.service;

import br.com.emr.emrfinancas.dto.*;
import br.com.emr.emrfinancas.exception.RegraNegocioException;
import br.com.emr.emrfinancas.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProfileService {
    private final AuthenticatedUserService authenticated;
    private final UsuarioRepository repository;
    private final PasswordEncoder encoder;
    private final PasswordPolicyValidator policy;

    public ProfileService(AuthenticatedUserService authenticated, UsuarioRepository repository,
                          PasswordEncoder encoder, PasswordPolicyValidator policy) {
        this.authenticated = authenticated;
        this.repository = repository;
        this.encoder = encoder;
        this.policy = policy;
    }

    @Transactional
    public UsuarioResponse update(UpdateProfileRequest request) {
        var user = authenticated.getAuthenticatedUser();
        String nome = request.getNome().trim();
        if (nome.length() < 2) throw new RegraNegocioException("Informe um nome com pelo menos 2 caracteres");
        user.setNome(nome);
        return UsuarioResponse.from(repository.save(user));
    }

    @Transactional
    public void changePassword(ChangePasswordRequest request) {
        var user = authenticated.getAuthenticatedUser();
        if (!encoder.matches(request.senhaAtual(), user.getSenha())) {
            throw new RegraNegocioException("Senha atual incorreta");
        }
        policy.validate(request.novaSenha());
        user.setSenha(encoder.encode(request.novaSenha()));
        repository.save(user);
    }
}
