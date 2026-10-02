package com.cliniva.booking.dtos;

import java.time.LocalDateTime;
import java.util.UUID;

public record BookingResponseDTO(
        UUID id,
        LocalDateTime dataAtendimento,
        Integer duracaoMinutos,
        String servico,
        String clinica,
        String cliente) {

}