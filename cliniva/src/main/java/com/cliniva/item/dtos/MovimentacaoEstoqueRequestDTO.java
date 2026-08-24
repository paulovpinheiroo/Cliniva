package com.cliniva.item.dtos;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record MovimentacaoEstoqueRequestDTO(
                @NotNull(message = "Tipo de movimentação é obrigatório")
                TipoMovimentacao tipo,

                @NotNull(message = "Quantidade é obrigatória")
                @Positive(message = "Quantidade deve ser positiva")
                BigDecimal quantidade) {

        public enum TipoMovimentacao {
                ENTRADA, SAIDA
        }
}
