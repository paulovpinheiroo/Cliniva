package com.cliniva.atendimento.dtos;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.cliniva.atendimento.enums.StatusAtendimento;

public record AtendimentoResponseDTO(
        UUID id,
        UUID clienteId,
        String nomeCliente,
        LocalDateTime dataAtendimento,
        LocalDate dataCriacao,
        StatusAtendimento status,
        BigDecimal valorTotal,
        List<ServicoRealizadoDTO> servicos) {

    public record ServicoRealizadoDTO(
            UUID servicoId,
            String nomeServico,
            BigDecimal valorCobrado,
            List<ItemUsadoRealDTO> itensUsados) {
        public record ItemUsadoRealDTO(UUID itemId, String nomeItem, BigDecimal quantidadeUsada) {
        }
    }
}
