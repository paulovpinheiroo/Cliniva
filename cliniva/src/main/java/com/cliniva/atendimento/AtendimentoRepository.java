package com.cliniva.atendimento;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AtendimentoRepository extends JpaRepository<Atendimento, UUID> {

}
