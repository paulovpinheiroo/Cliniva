package com.cliniva.item.dtos;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateItemResponseDTO(
        UUID id,
        String nome,
        BigDecimal quantidadeEmEstoque) {

}
