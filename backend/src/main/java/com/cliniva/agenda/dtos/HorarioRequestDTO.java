package com.cliniva.agenda.dtos;

import java.time.LocalTime;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * @param diaSemana 1 (segunda) a 7 (domingo) — obrigatório.
 * @param ativo     obrigatório: se omitir num PUT, o dia seria fechado sem
 *                  querer (bug do Jackson com boolean primitivo).
 */
public record HorarioRequestDTO(
        @NotNull(message = "Dia da semana é obrigatório")
        @Min(value = 1, message = "Dia da semana deve ser entre 1 (segunda) e 7 (domingo)")
        @Max(value = 7, message = "Dia da semana deve ser entre 1 (segunda) e 7 (domingo)")
        Integer diaSemana,

        @NotNull(message = "Abertura é obrigatória")
        LocalTime abertura,

        @NotNull(message = "Fechamento é obrigatório")
        LocalTime fechamento,

        @NotNull(message = "Indique se o dia está ativo")
        Boolean ativo) {

    public boolean ativoOuPadrao() {
        return ativo != null && ativo;
    }
}
