package com.cliniva.cliente.dtos;

import java.util.UUID;

public record CreateClienteResponseDTO(
                UUID id,
                String nome,
                String email,
                String telefone) {

}
