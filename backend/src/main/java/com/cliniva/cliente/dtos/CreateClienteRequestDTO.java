package com.cliniva.cliente.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateClienteRequestDTO(
                @NotBlank(message = "Nome é obrigatório")
                @Size(max = 120, message = "Nome deve ter no máximo 120 caracteres")
                String nome,

                @Email(message = "Email inválido")
                @Size(max = 120, message = "Email deve ter no máximo 120 caracteres")
                String email,

                @NotBlank(message = "Telefone é obrigatório")
                @Size(max = 20, message = "Telefone deve ter no máximo 20 caracteres")
                String telefone) {

}
