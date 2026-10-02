package com.cliniva.booking.dtos;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record BookingRequestDTO(
        @NotNull(message = "Serviço é obrigatório")
        UUID servicoId,

        @NotNull(message = "Data e hora são obrigatórias")
        LocalDateTime dataHora,

        @NotBlank(message = "Nome é obrigatório")
        @Size(min = 3, max = 120, message = "Nome deve ter entre 3 e 120 caracteres")
        String nome,

        @NotBlank(message = "Telefone é obrigatório")
        @Pattern(regexp = "[0-9+()\\-\\s]{8,20}",
                message = "Telefone inválido (use apenas números, DDD e DDI)")
        String telefone,

        @Email(message = "E-mail inválido")
        @Size(max = 120, message = "E-mail deve ter no máximo 120 caracteres")
        String email) {

}
