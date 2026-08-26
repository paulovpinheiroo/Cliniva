package com.cliniva.cliente.dtos;

import java.util.UUID;

public record ClienteResponseDTO(
                UUID id,
                String nome,
                String email,
                String telefone) {

}
