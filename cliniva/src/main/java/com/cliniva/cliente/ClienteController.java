package com.cliniva.cliente;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.cliniva.cliente.dtos.CreateClienteRequestDTO;
import com.cliniva.cliente.dtos.CreateClienteResponseDTO;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/clientes")
@RequiredArgsConstructor
public class ClienteController {
    private final ClienteService clienteService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CreateClienteResponseDTO createCliente(@RequestBody CreateClienteRequestDTO createClienteRequestDTO) {
        return clienteService.createCliente(createClienteRequestDTO);
    }

}
