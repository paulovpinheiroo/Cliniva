package com.cliniva.item.dtos;

import java.math.BigDecimal;

import jakarta.validation.constraints.PositiveOrZero;

public record CreateItemRequestDTO(
                String nome,
                @PositiveOrZero BigDecimal quantidadeEmEstoque) {

}
