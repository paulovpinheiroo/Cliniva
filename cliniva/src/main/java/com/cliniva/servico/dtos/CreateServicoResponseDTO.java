package com.cliniva.servico.dtos;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateServicoResponseDTO(
                UUID id,
                String nome,
                String descricao,
                BigDecimal valor) {

}
