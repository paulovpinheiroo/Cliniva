package com.cliniva.cliente.dtos;

import java.time.LocalDateTime;
import java.util.UUID;

public record NotaResponseDTO(
                UUID id,
                String texto,
                LocalDateTime criadaEm) {

}