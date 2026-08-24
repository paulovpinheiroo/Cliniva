package com.cliniva.servico.dtos;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record UpdateServicoRequestDTO(
                @NotBlank(message = "Nome é obrigatório")
                @Size(max = 120, message = "Nome deve ter no máximo 120 caracteres")
                String nome,

                @Size(max = 500, message = "Descrição deve ter no máximo 500 caracteres")
                String descricao,

                @NotNull(message = "Valor é obrigatório")
                @Positive(message = "Valor deve ser positivo")
                BigDecimal valor) {

}
