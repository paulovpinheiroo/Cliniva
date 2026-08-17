package com.cliniva.atendimento.dtos;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.cliniva.atendimento.enums.StatusAtendimento;

public record CreateAtendimentoResponseDTO(
        UUID id,
        UUID clienteId,
        LocalDateTime dataAtendimento,
        LocalDate dataCriacao,
        StatusAtendimento status,
        List<ServicoRealizadoDTO> servicos) {

    public record ServicoRealizadoDTO(
            UUID servicoId,
            BigDecimal valorCobrado,
            List<ItemUsadoRealDTO> itensExtras) {
        public record ItemUsadoRealDTO(UUID itemId, BigDecimal quantidadeUsada) {
        }
    }
}