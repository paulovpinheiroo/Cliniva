package com.cliniva.atendimento.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cliniva.atendimento.model.Atendimento;
import com.cliniva.atendimento.model.AtendimentoServico;
import com.cliniva.servico.Servico;

public interface AtendimentoServicoRepository extends JpaRepository<AtendimentoServico, UUID> {

    boolean existsByAtendimentoAndServico(Atendimento atendimento, Servico servico);

    List<AtendimentoServico> findByAtendimento(Atendimento atendimento);

    boolean existsByServico_Id(UUID servicoId);
}
