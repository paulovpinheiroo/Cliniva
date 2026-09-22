package com.cliniva.booking.dtos;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record BookingRequestDTO(
        @NotNull(message = "Serviço é obrigatório")
        UUID servicoId,

        @NotNull(message = "Data e hora são obrigatórias")
        LocalDateTime dataHora,

        @NotBlank(message = "Nome é obrigatório")
        String nome,

        @NotBlank(message = "Telefone é obrigatório")
        String telefone,

        @Email(message = "E-mail inválido")
        String email) {

}