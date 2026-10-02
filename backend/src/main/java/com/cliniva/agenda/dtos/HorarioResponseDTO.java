package com.cliniva.agenda.dtos;

import java.time.LocalTime;

public record HorarioResponseDTO(
        Integer diaSemana,
        LocalTime abertura,
        LocalTime fechamento,
        boolean ativo) {

}