package br.com.emr.emrfinancas.repository;

        import br.com.emr.emrfinancas.model.Usuario;
        import org.springframework.data.jpa.repository.JpaRepository;
        import java.util.Optional;
        import org.springframework.data.jpa.repository.Modifying;
        import org.springframework.data.jpa.repository.Query;
        import org.springframework.data.repository.query.Param;
        import org.springframework.transaction.annotation.Transactional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByEmail(String email);
    Optional<Usuario> findByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCase(String email);

    // Update only lockout state; a detached snapshot must not overwrite password/role/tokenVersion.
    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Usuario u set u.failedLoginAttempts = 0, u.lockedUntil = null where u.codigo = :codigo")
    int resetLoginFailuresByCodigo(@Param("codigo") Long codigo);
}
