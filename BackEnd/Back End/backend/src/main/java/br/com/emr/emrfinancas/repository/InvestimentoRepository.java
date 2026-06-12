package br.com.emr.emrfinancas.repository;

import br.com.emr.emrfinancas.model.Investimento;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvestimentoRepository extends JpaRepository<Investimento, Long> {}
