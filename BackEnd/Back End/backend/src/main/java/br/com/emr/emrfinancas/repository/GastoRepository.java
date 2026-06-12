package br.com.emr.emrfinancas.repository;

import br.com.emr.emrfinancas.model.Gasto;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GastoRepository extends JpaRepository<Gasto, Long> {}
