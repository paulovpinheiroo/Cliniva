package com.cliniva.cliente.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateClienteNotaRequestDTO(
                @NotBlank(message = "Texto da nota é obrigatório")
                @Size(max = 2000, message = "Nota deve ter no máximo 2000 caracteres")
                String texto) {

}