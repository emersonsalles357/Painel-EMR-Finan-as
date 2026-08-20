package br.com.emr.emrfinancas.repository;

        import br.com.emr.emrfinancas.model.Usuario;
        import org.springframework.data.jpa.repository.JpaRepository;
        import java.util.Optional;

        public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByEmailAndSenha(String email, String senha);
    Optional<Usuario> findByEmail(String email);
}
