package com.cliniva.agenda.dtos;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record DisponibilidadeDiaDTO(
        LocalDate data,
        Integer duracaoMinutos,
        List<LocalTime> horarios) {

}