package com.cliniva.booking.dtos;

import java.math.BigDecimal;
import java.util.UUID;

public record ServicoPublicoDTO(
        UUID id,
        String nome,
        String descricao,
        BigDecimal valor,
        Integer duracaoMinutos,
        String clinica) {

}