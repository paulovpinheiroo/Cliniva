package com.cliniva.atendimento;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import com.cliniva.atendimento.enums.StatusAtendimento;

public interface AtendimentoRepository extends JpaRepository<Atendimento, UUID> {

    List<Atendimento> findByDataAtendimentoBetween(LocalDateTime inicio, LocalDateTime fim);

    List<Atendimento> findByStatus(StatusAtendimento status);

    List<Atendimento> findByCliente_Id(UUID clienteId);
}
