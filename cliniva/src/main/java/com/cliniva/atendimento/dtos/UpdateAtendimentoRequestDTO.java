package com.cliniva.atendimento.dtos;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record UpdateAtendimentoRequestDTO(
                @NotNull(message = "Cliente é obrigatório")
                UUID clienteId,

                @NotNull(message = "Data do atendimento é obrigatória")
                LocalDateTime dataAtendimento) {

}
