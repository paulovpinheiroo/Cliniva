package com.cliniva.atendimento.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import java.time.LocalDateTime;
import com.cliniva.atendimento.enums.StatusAtendimento;
import com.cliniva.atendimento.model.Atendimento;

public interface AtendimentoRepository
        extends JpaRepository<Atendimento, UUID>, JpaSpecificationExecutor<Atendimento> {

    List<Atendimento> findByDataAtendimentoBetween(LocalDateTime inicio, LocalDateTime fim);

    List<Atendimento> findByStatus(StatusAtendimento status);

    List<Atendimento> findByCliente_Id(UUID clienteId);

    boolean existsByCliente_Id(UUID clienteId);
}
