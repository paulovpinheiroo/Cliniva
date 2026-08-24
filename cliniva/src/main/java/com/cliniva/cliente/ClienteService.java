package com.cliniva.cliente;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cliniva.cliente.dtos.ClienteResponseDTO;
import com.cliniva.cliente.dtos.CreateClienteRequestDTO;
import com.cliniva.cliente.dtos.CreateClienteResponseDTO;
import com.cliniva.cliente.dtos.UpdateClienteRequestDTO;
import com.cliniva.atendimento.repository.AtendimentoRepository;
import com.cliniva.exception.RecursoDuplicadoException;
import com.cliniva.exception.RecursoEmUsoException;
import com.cliniva.exception.RecursoNaoEncontradoException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor

public class ClienteService {
    private final ClienteRepository clienteRepository;
    private final AtendimentoRepository atendimentoRepository;

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

    @Transactional(readOnly = true)
    public List<ClienteResponseDTO> listarClientes(String nome) {
        List<Cliente> clientes = (nome == null || nome.isBlank())
                ? clienteRepository.findAll()
                : clienteRepository.findByNomeContainingIgnoreCase(nome);
        return clientes.stream().map(this::toResponseDTO).toList();
    }

    @Transactional(readOnly = true)
    public ClienteResponseDTO buscarPorId(UUID id) {
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cliente não encontrado"));
        return toResponseDTO(cliente);
    }

    @Transactional
    public ClienteResponseDTO atualizarCliente(UUID id, UpdateClienteRequestDTO requestDTO) {
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cliente não encontrado"));

        if (requestDTO.email() != null && !requestDTO.email().isBlank()
                && clienteRepository.existsByEmailAndIdNot(requestDTO.email(), id)) {
            throw new RecursoDuplicadoException("Email já cadastrado");
        }
        if (clienteRepository.existsByTelefoneAndIdNot(requestDTO.telefone(), id)) {
            throw new RecursoDuplicadoException("Telefone já cadastrado");
        }

        cliente.setNome(requestDTO.nome());
        cliente.setEmail(requestDTO.email());
        cliente.setTelefone(requestDTO.telefone());

        return toResponseDTO(clienteRepository.save(cliente));
    }

    @Transactional
    public void deletarCliente(UUID id) {
        if (!clienteRepository.existsById(id)) {
            throw new RecursoNaoEncontradoException("Cliente não encontrado");
        }
        if (atendimentoRepository.existsByCliente_Id(id)) {
            throw new RecursoEmUsoException("Cliente possui atendimentos vinculados e não pode ser excluído");
        }
        clienteRepository.deleteById(id);
    }

    private ClienteResponseDTO toResponseDTO(Cliente cliente) {
        return new ClienteResponseDTO(cliente.getId(), cliente.getNome(), cliente.getEmail(), cliente.getTelefone());
    }

}
