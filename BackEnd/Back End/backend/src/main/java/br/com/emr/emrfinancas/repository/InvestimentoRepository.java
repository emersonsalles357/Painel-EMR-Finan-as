package br.com.emr.emrfinancas.repository;

import br.com.emr.emrfinancas.model.Investimento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface InvestimentoRepository extends JpaRepository<Investimento, Long> {
    List<Investimento> findAllByUsuarioCodigo(Long usuarioId);

    Optional<Investimento> findByCodigoAndUsuarioCodigo(Long codigo, Long usuarioId);

    @Query("select coalesce(sum(i.valorAplicado), 0) from Investimento i where i.usuario.codigo = :usuarioId")
    BigDecimal somarPorUsuario(@Param("usuarioId") Long usuarioId);
}
