package com.cliniva.atendimento.dtos;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import com.cliniva.atendimento.enums.StatusAtendimento;

public record AtendimentoResumoResponseDTO(
        UUID id,
        String nomeCliente,
        LocalDateTime dataAtendimento,
        StatusAtendimento status,
        BigDecimal valorTotal) {

}
