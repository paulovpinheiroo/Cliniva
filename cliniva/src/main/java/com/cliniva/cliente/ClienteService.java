package com.cliniva.cliente;

import org.springframework.stereotype.Service;

import com.cliniva.cliente.dtos.CreateClienteRequestDTO;
import com.cliniva.cliente.dtos.CreateClienteResponseDTO;
import com.cliniva.exception.RecursoDuplicadoException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor

public class ClienteService {
    private final ClienteRepository clienteRepository;

    public CreateClienteResponseDTO createCliente(CreateClienteRequestDTO createClienteDTO) {
        if (clienteRepository.existsByEmail(createClienteDTO.email())) {
            throw new RecursoDuplicadoException("Email já cadastrado");
        }
        if (clienteRepository.existsByTelefone(createClienteDTO.telefone())) {
            throw new RecursoDuplicadoException("Telefone já cadastrado");
        }

        Cliente cliente = new Cliente();
        cliente.setNome(createClienteDTO.nome());
        cliente.setEmail(createClienteDTO.email());
        cliente.setTelefone(createClienteDTO.telefone());

        Cliente savedCliente = clienteRepository.save(cliente);

        return new CreateClienteResponseDTO(savedCliente.getId(), savedCliente.getNome(), savedCliente.getEmail(),
                savedCliente.getTelefone());
    }

}
