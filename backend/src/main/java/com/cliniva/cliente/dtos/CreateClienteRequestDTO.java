package com.cliniva.cliente.dtos;

import java.time.LocalDate;

import com.cliniva.cliente.enums.CanalPreferido;
import com.cliniva.cliente.enums.OrigemCliente;

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
                String telefone,

                LocalDate dataNascimento,
                OrigemCliente origem,
                CanalPreferido canalPreferido,
                @Size(max = 500, message = "Preferências devem ter no máximo 500 caracteres")
                String preferencias,
                @Size(max = 2000, message = "Observações devem ter no máximo 2000 caracteres")
                String observacoes) {

}