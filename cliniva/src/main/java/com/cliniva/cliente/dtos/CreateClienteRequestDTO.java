package com.cliniva.cliente.dtos;

public record CreateClienteRequestDTO(
        String nome,
        String email,
        String telefone) {

}
