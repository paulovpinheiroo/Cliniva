package com.cliniva.atendimento.dtos;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record CreateAtendimentoRequestDTO(
                UUID clienteId,
                LocalDateTime dataAtendimento,
                List<ServicoSelecionadoDTO> servicos) {
        public record ServicoSelecionadoDTO(
                        UUID servicoId,
                        List<ItemUsadoDTO> itensExtras) {
                public record ItemUsadoDTO(UUID itemId, BigDecimal quantidade) {
                }
        }
}
