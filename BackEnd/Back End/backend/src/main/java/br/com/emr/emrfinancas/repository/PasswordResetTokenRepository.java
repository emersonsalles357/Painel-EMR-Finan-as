package br.com.emr.emrfinancas.repository;

import br.com.emr.emrfinancas.model.PasswordResetToken;
import br.com.emr.emrfinancas.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    Optional<PasswordResetToken> findByTokenHash(String tokenHash);

    List<PasswordResetToken> findAllByUsuarioAndUsedAtIsNull(Usuario usuario);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE PasswordResetToken t SET t.usedAt = :now WHERE t.usuario = :usuario AND t.usedAt IS NULL")
    int invalidateActiveTokensForUser(@Param("usuario") Usuario usuario, @Param("now") Instant now);
}
