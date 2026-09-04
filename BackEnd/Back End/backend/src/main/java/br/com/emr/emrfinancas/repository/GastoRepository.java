package br.com.emr.emrfinancas.repository;

import br.com.emr.emrfinancas.model.Gasto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface GastoRepository extends JpaRepository<Gasto, Long> {
    List<Gasto> findAllByUsuarioCodigo(Long usuarioId);

    Optional<Gasto> findByCodigoAndUsuarioCodigo(Long codigo, Long usuarioId);

    @Query("select coalesce(sum(g.valor), 0) from Gasto g where g.usuario.codigo = :usuarioId")
    BigDecimal somarPorUsuario(@Param("usuarioId") Long usuarioId);
}
