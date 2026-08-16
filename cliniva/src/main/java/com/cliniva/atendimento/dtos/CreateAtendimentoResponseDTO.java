package com.cliniva.atendimento.dtos;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.cliniva.atendimento.enums.StatusAtendimento;

public record CreateAtendimentoResponseDTO(
        UUID atendimentoId,
        UUID clienteId,
        LocalDateTime dataAtendimento,
        LocalDate dataCriacao,
        List<ServicoSelecionadoDTO> servico,
        StatusAtendimento status) {
    public record ServicoSelecionadoDTO(
            UUID servicoId,
            List<ItemUsadoDTO> itensExtras) {
        public record ItemUsadoDTO(UUID itemId, BigDecimal quantidade) {

        }
    }
}