package com.cliniva.atendimento.repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.cliniva.atendimento.enums.StatusAtendimento;
import com.cliniva.atendimento.model.Atendimento;
import com.cliniva.atendimento.model.AtendimentoServico;
import com.cliniva.servico.Servico;

public interface AtendimentoServicoRepository extends JpaRepository<AtendimentoServico, UUID> {

    boolean existsByAtendimentoAndServico(Atendimento atendimento, Servico servico);

    List<AtendimentoServico> findByAtendimento(Atendimento atendimento);

    boolean existsByServico_Id(UUID servicoId);

    @Query("SELECT ats.atendimento.clinica.id, COALESCE(SUM(ats.valorCobrado), 0) "
            + "FROM com.cliniva.atendimento.model.AtendimentoServico ats JOIN ats.atendimento a "
            + "WHERE a.status = :status GROUP BY ats.atendimento.clinica.id")
    List<Object[]> totalCobradoPorClinica(@Param("status") StatusAtendimento status);
}
