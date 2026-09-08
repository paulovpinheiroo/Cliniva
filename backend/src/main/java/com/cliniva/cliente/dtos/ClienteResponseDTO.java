package com.cliniva.cliente.dtos;

import java.time.LocalDate;
import java.util.UUID;

import com.cliniva.cliente.enums.CanalPreferido;
import com.cliniva.cliente.enums.ClienteStatus;
import com.cliniva.cliente.enums.OrigemCliente;

public record ClienteResponseDTO(
                UUID id,
                String nome,
                String email,
                String telefone,
                LocalDate dataNascimento,
                ClienteStatus status,
                OrigemCliente origem,
                CanalPreferido canalPreferido,
                String preferencias,
                String observacoes) {

}