package com.cliniva.servico.dtos;

import java.math.BigDecimal;

public record CreateServicoRequestDTO(
                String nome,
                String descricao,
                BigDecimal valor) {

}
