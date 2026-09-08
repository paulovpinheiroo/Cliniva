package com.cliniva.cliente;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cliniva.atendimento.AtendimentoService;
import com.cliniva.atendimento.dtos.AtendimentoResponseDTO;
import com.cliniva.atendimento.enums.StatusAtendimento;
import com.cliniva.atendimento.model.Atendimento;
import com.cliniva.atendimento.repository.AtendimentoRepository;
import com.cliniva.cliente.dtos.ClienteHistoricoResponseDTO;
import com.cliniva.cliente.dtos.ClienteResponseDTO;
import com.cliniva.cliente.dtos.CreateClienteNotaRequestDTO;
import com.cliniva.cliente.dtos.CreateClienteRequestDTO;
import com.cliniva.cliente.dtos.CreateClienteResponseDTO;
import com.cliniva.cliente.dtos.NotaResponseDTO;
import com.cliniva.cliente.dtos.UpdateClienteRequestDTO;
import com.cliniva.cliente.enums.ClienteStatus;
import com.cliniva.exception.RecursoDuplicadoException;
import com.cliniva.exception.RecursoEmUsoException;
import com.cliniva.exception.RecursoNaoEncontradoException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor

public class ClienteService {
    private final ClienteRepository clienteRepository;
    private final ClienteNotaRepository clienteNotaRepository;
    private final AtendimentoRepository atendimentoRepository;
    private final AtendimentoService atendimentoService;

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
        cliente.setDataNascimento(createClienteDTO.dataNascimento());
        cliente.setOrigem(createClienteDTO.origem());
        cliente.setCanalPreferido(createClienteDTO.canalPreferido());
        cliente.setPreferencias(createClienteDTO.preferencias());
        cliente.setObservacoes(createClienteDTO.observacoes());

        Cliente savedCliente = clienteRepository.save(cliente);

        return toCreateResponseDTO(savedCliente);
    }

    @Transactional(readOnly = true)
    public List<ClienteResponseDTO> listarClientes(String nome, ClienteStatus status) {
        List<Cliente> clientes;
        boolean temNome = nome != null && !nome.isBlank();
        if (temNome && status != null) {
            clientes = clienteRepository.findByNomeContainingIgnoreCaseAndStatus(nome, status);
        } else if (temNome) {
            clientes = clienteRepository.findByNomeContainingIgnoreCase(nome);
        } else if (status != null) {
            clientes = clienteRepository.findByStatus(status);
        } else {
            clientes = clienteRepository.findAll();
        }
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
        if (requestDTO.dataNascimento() != null) {
            cliente.setDataNascimento(requestDTO.dataNascimento());
        }
        if (requestDTO.status() != null) {
            cliente.setStatus(requestDTO.status());
        }
        if (requestDTO.origem() != null) {
            cliente.setOrigem(requestDTO.origem());
        }
        if (requestDTO.canalPreferido() != null) {
            cliente.setCanalPreferido(requestDTO.canalPreferido());
        }
        if (requestDTO.preferencias() != null) {
            cliente.setPreferencias(requestDTO.preferencias());
        }
        if (requestDTO.observacoes() != null) {
            cliente.setObservacoes(requestDTO.observacoes());
        }

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
        if (clienteNotaRepository.existsByCliente_Id(id)) {
            throw new RecursoEmUsoException("Cliente possui anotações vinculadas e não pode ser excluído");
        }
        clienteRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<NotaResponseDTO> listarNotas(UUID clienteId) {
        buscarPorId(clienteId);
        return clienteNotaRepository.findByCliente_IdOrderByCriadaEmDesc(clienteId).stream()
                .map(nota -> new NotaResponseDTO(nota.getId(), nota.getTexto(), nota.getCriadaEm()))
                .toList();
    }

    @Transactional
    public NotaResponseDTO criarNota(UUID clienteId, CreateClienteNotaRequestDTO requestDTO) {
        Cliente cliente = clienteRepository.findById(clienteId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cliente não encontrado"));

        ClienteNota nota = new ClienteNota();
        nota.setCliente(cliente);
        nota.setTexto(requestDTO.texto().trim());

        ClienteNota salva = clienteNotaRepository.save(nota);
        return new NotaResponseDTO(salva.getId(), salva.getTexto(), salva.getCriadaEm());
    }

    @Transactional
    public void deletarNota(UUID clienteId, UUID notaId) {
        if (!clienteRepository.existsById(clienteId)) {
            throw new RecursoNaoEncontradoException("Cliente não encontrado");
        }
        ClienteNota nota = clienteNotaRepository.findById(notaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Nota não encontrada"));
        if (!nota.getCliente().getId().equals(clienteId)) {
            throw new RecursoNaoEncontradoException("Nota não encontrada");
        }
        clienteNotaRepository.delete(nota);
    }

    @Transactional(readOnly = true)
    public ClienteHistoricoResponseDTO historicoCliente(UUID id) {
        ClienteResponseDTO perfil = buscarPorId(id);

        List<AtendimentoResponseDTO> atendimentos = atendimentoRepository.findByCliente_Id(id).stream()
                .sorted(Comparator.comparing(Atendimento::getDataAtendimento).reversed())
                .map(atendimento -> atendimentoService.buscarPorId(atendimento.getId()))
                .toList();

        long totalAtendimentos = atendimentos.size();
        List<AtendimentoResponseDTO> concluidos = atendimentos.stream()
                .filter(atendimento -> atendimento.status() == StatusAtendimento.CONCLUIDO)
                .toList();
        long atendimentosConcluidos = concluidos.size();
        BigDecimal gastoTotal = concluidos.stream()
                .map(AtendimentoResponseDTO::valorTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal ticketMedio = atendimentosConcluidos > 0
                ? gastoTotal.divide(BigDecimal.valueOf(atendimentosConcluidos), 2, RoundingMode.HALF_UP)
                : null;
        LocalDate ultimaVisita = concluidos.stream()
                .map(AtendimentoResponseDTO::dataAtendimento)
                .map(java.time.LocalDateTime::toLocalDate)
                .max(Comparator.naturalOrder())
                .orElse(null);

        return new ClienteHistoricoResponseDTO(
                perfil,
                totalAtendimentos,
                atendimentosConcluidos,
                gastoTotal,
                ticketMedio,
                ultimaVisita,
                atendimentos);
    }

    @Transactional(readOnly = true)
    public List<ClienteResponseDTO> aniversariantes(Integer mes) {
        int mesConsulta = mes != null ? mes : LocalDate.now().getMonthValue();
        if (mesConsulta < 1 || mesConsulta > 12) {
            throw new IllegalArgumentException("Mês deve estar entre 1 e 12");
        }
        return clienteRepository.findByDataNascimentoMes(mesConsulta).stream()
                .sorted(Comparator.comparing(Cliente::getDataNascimento))
                .map(this::toResponseDTO)
                .toList();
    }

    private CreateClienteResponseDTO toCreateResponseDTO(Cliente cliente) {
        return new CreateClienteResponseDTO(
                cliente.getId(),
                cliente.getNome(),
                cliente.getEmail(),
                cliente.getTelefone(),
                cliente.getDataNascimento(),
                cliente.getStatus(),
                cliente.getOrigem(),
                cliente.getCanalPreferido(),
                cliente.getPreferencias(),
                cliente.getObservacoes());
    }

    private ClienteResponseDTO toResponseDTO(Cliente cliente) {
        return new ClienteResponseDTO(
                cliente.getId(),
                cliente.getNome(),
                cliente.getEmail(),
                cliente.getTelefone(),
                cliente.getDataNascimento(),
                cliente.getStatus(),
                cliente.getOrigem(),
                cliente.getCanalPreferido(),
                cliente.getPreferencias(),
                cliente.getObservacoes());
    }

}