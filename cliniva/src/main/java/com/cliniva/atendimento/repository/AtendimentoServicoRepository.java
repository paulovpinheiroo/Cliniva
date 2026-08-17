package com.cliniva.atendimento.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cliniva.atendimento.model.Atendimento;
import com.cliniva.atendimento.model.AtendimentoServico;
import com.cliniva.servico.Servico;

public interface AtendimentoServicoRepository extends JpaRepository<AtendimentoServico, UUID> {

    boolean existsByAtendimentoAndServico(Atendimento atendimento, Servico servico);
}
