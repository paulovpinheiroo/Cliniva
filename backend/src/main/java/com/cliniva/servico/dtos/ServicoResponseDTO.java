package com.cliniva.servico.dtos;

import java.math.BigDecimal;
import java.util.UUID;

public record ServicoResponseDTO(
                UUID id,
                String nome,
                String descricao,
                BigDecimal valor) {

}
