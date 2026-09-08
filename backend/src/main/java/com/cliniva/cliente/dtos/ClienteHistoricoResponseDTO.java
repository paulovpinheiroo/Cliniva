package com.cliniva.cliente.dtos;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.cliniva.atendimento.dtos.AtendimentoResponseDTO;

public record ClienteHistoricoResponseDTO(
                ClienteResponseDTO cliente,
                long totalAtendimentos,
                long atendimentosConcluidos,
                BigDecimal gastoTotal,
                BigDecimal ticketMedio,
                LocalDate ultimaVisita,
                List<AtendimentoResponseDTO> atendimentos) {

}