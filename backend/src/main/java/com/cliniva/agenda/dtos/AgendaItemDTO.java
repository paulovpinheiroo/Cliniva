package com.cliniva.agenda.dtos;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.cliniva.atendimento.enums.StatusAtendimento;

public record AgendaItemDTO(
        UUID id,
        UUID clienteId,
        String clienteNome,
        String clienteTelefone,
        LocalDateTime inicio,
        LocalDateTime fim,
        Integer duracaoMinutos,
        StatusAtendimento status,
        BigDecimal valorTotal,
        List<String> servicos) {

}