package com.cliniva.agenda.dtos;

import java.time.LocalTime;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record HorarioRequestDTO(
        @Min(value = 1, message = "Dia da semana deve ser entre 1 (segunda) e 7 (domingo)")
        @Max(value = 7, message = "Dia da semana deve ser entre 1 (segunda) e 7 (domingo)")
        Integer diaSemana,

        @NotNull(message = "Abertura é obrigatória")
        LocalTime abertura,

        @NotNull(message = "Fechamento é obrigatório")
        LocalTime fechamento,

        boolean ativo) {

}