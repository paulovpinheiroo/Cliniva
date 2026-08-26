package com.cliniva.atendimento.dtos;

import com.cliniva.atendimento.enums.StatusAtendimento;

import jakarta.validation.constraints.NotNull;

public record UpdateStatusAtendimentoRequestDTO(
                @NotNull(message = "Novo status é obrigatório")
                StatusAtendimento novoStatus) {

}
