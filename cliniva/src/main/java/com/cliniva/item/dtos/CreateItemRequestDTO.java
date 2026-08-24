package com.cliniva.item.dtos;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record CreateItemRequestDTO(
                @NotBlank(message = "Nome é obrigatório")
                @Size(max = 120, message = "Nome deve ter no máximo 120 caracteres")
                String nome,
                @PositiveOrZero(message = "Quantidade em estoque não pode ser negativa")
                BigDecimal quantidadeEmEstoque) {

}
