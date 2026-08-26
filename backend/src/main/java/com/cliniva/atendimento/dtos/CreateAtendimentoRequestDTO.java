package com.cliniva.atendimento.dtos;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreateAtendimentoRequestDTO(
                @NotNull(message = "Cliente é obrigatório")
                UUID clienteId,

                @NotNull(message = "Data do atendimento é obrigatória")
                LocalDateTime dataAtendimento,

                @Size(min = 1, message = "Atendimento deve ter pelo menos um serviço")
                @Valid
                List<ServicoSelecionadoDTO> servicos) {
        public record ServicoSelecionadoDTO(
                        @NotNull(message = "Serviço é obrigatório")
                        UUID servicoId,
                        @Valid
                        List<ItemUsadoDTO> itensExtras) {
                public record ItemUsadoDTO(
                                @NotNull(message = "Item é obrigatório")
                                UUID itemId,
                                @NotNull(message = "Quantidade é obrigatória")
                                @Positive(message = "Quantidade deve ser positiva")
                                BigDecimal quantidade) {
                }
        }
}
