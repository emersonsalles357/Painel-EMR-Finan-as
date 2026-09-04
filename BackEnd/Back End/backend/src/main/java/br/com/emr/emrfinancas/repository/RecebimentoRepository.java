package br.com.emr.emrfinancas.repository;

import br.com.emr.emrfinancas.model.Recebimento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface RecebimentoRepository extends JpaRepository<Recebimento, Long> {
    List<Recebimento> findAllByUsuarioCodigo(Long usuarioId);

    Optional<Recebimento> findByCodigoAndUsuarioCodigo(Long codigo, Long usuarioId);

    @Query("select coalesce(sum(r.valor), 0) from Recebimento r where r.usuario.codigo = :usuarioId")
    BigDecimal somarPorUsuario(@Param("usuarioId") Long usuarioId);
}
