package com.cliniva.cliente;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.cliniva.cliente.dtos.ClienteResponseDTO;
import com.cliniva.cliente.dtos.CreateClienteRequestDTO;
import com.cliniva.cliente.dtos.CreateClienteResponseDTO;
import com.cliniva.cliente.dtos.UpdateClienteRequestDTO;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/clientes")
@RequiredArgsConstructor
public class ClienteController {
    private final ClienteService clienteService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CreateClienteResponseDTO createCliente(
                    @Valid @RequestBody CreateClienteRequestDTO createClienteRequestDTO) {
        return clienteService.createCliente(createClienteRequestDTO);
    }

    @GetMapping
    public List<ClienteResponseDTO> listarClientes(
                    @RequestParam(required = false) String nome) {
        return clienteService.listarClientes(nome);
    }

    @GetMapping("/{id}")
    public ClienteResponseDTO buscarPorId(@PathVariable UUID id) {
        return clienteService.buscarPorId(id);
    }

    @PutMapping("/{id}")
    public ClienteResponseDTO atualizarCliente(
                    @PathVariable UUID id,
                    @Valid @RequestBody UpdateClienteRequestDTO requestDTO) {
        return clienteService.atualizarCliente(id, requestDTO);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletarCliente(@PathVariable UUID id) {
        clienteService.deletarCliente(id);
    }

}
