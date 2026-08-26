package com.cliniva.item.dtos;

import java.math.BigDecimal;
import java.util.UUID;

public record ItemResponseDTO(
                UUID id,
                String nome,
                BigDecimal quantidadeEmEstoque) {

}
