package com.cliniva.item.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateItemRequestDTO(
                @NotBlank(message = "Nome é obrigatório")
                @Size(max = 120, message = "Nome deve ter no máximo 120 caracteres")
                String nome) {

}
