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
import com.cliniva.tenancy.Clinica;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor

public class ClienteService {
    private final ClienteRepository clienteRepository;
    private final ClienteNotaRepository clienteNotaRepository;
    private final AtendimentoRepository atendimentoRepository;
    private final AtendimentoService atendimentoService;

    public CreateClienteResponseDTO createCliente(Clinica clinica, CreateClienteRequestDTO createClienteDTO) {
        if (clienteRepository.existsByEmailAndClinica(createClienteDTO.email(), clinica)) {
            throw new RecursoDuplicadoException("Email já cadastrado");
        }
        if (clienteRepository.existsByTelefoneAndClinica(createClienteDTO.telefone(), clinica)) {
            throw new RecursoDuplicadoException("Telefone já cadastrado");
        }

        Cliente cliente = new Cliente();
        cliente.setClinica(clinica);
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
    public List<ClienteResponseDTO> listarClientes(Clinica clinica, String nome, ClienteStatus status) {
        List<Cliente> clientes;
        boolean temNome = nome != null && !nome.isBlank();
        if (temNome && status != null) {
            clientes = clienteRepository.findByClinicaAndNomeContainingIgnoreCaseAndStatus(clinica, nome, status);
        } else if (temNome) {
            clientes = clienteRepository.findByClinicaAndNomeContainingIgnoreCase(clinica, nome);
        } else if (status != null) {
            clientes = clienteRepository.findByClinicaAndStatus(clinica, status);
        } else {
            clientes = clienteRepository.findByClinica(clinica);
        }
        return clientes.stream().map(this::toResponseDTO).toList();
    }

    @Transactional(readOnly = true)
    public ClienteResponseDTO buscarPorId(Clinica clinica, UUID id) {
        Cliente cliente = clienteRepository.findByIdAndClinica(id, clinica)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cliente não encontrado"));
        return toResponseDTO(cliente);
    }

    @Transactional
    public ClienteResponseDTO atualizarCliente(Clinica clinica, UUID id, UpdateClienteRequestDTO requestDTO) {
        Cliente cliente = clienteRepository.findByIdAndClinica(id, clinica)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cliente não encontrado"));

        if (requestDTO.email() != null && !requestDTO.email().isBlank()
                && clienteRepository.existsByEmailAndIdNotAndClinica(requestDTO.email(), id, clinica)) {
            throw new RecursoDuplicadoException("Email já cadastrado");
        }
        if (clienteRepository.existsByTelefoneAndIdNotAndClinica(requestDTO.telefone(), id, clinica)) {
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
    public void deletarCliente(Clinica clinica, UUID id) {
        if (!clienteRepository.existsByIdAndClinica(id, clinica)) {
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
    public List<NotaResponseDTO> listarNotas(Clinica clinica, UUID clienteId) {
        buscarPorId(clinica, clienteId);
        return clienteNotaRepository.findByCliente_IdOrderByCriadaEmDesc(clienteId).stream()
                .map(nota -> new NotaResponseDTO(nota.getId(), nota.getTexto(), nota.getCriadaEm()))
                .toList();
    }

    @Transactional
    public NotaResponseDTO criarNota(Clinica clinica, UUID clienteId, CreateClienteNotaRequestDTO requestDTO) {
        Cliente cliente = clienteRepository.findByIdAndClinica(clienteId, clinica)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cliente não encontrado"));

        ClienteNota nota = new ClienteNota();
        nota.setCliente(cliente);
        nota.setTexto(requestDTO.texto().trim());

        ClienteNota salva = clienteNotaRepository.save(nota);
        return new NotaResponseDTO(salva.getId(), salva.getTexto(), salva.getCriadaEm());
    }

    @Transactional
    public void deletarNota(Clinica clinica, UUID clienteId, UUID notaId) {
        if (!clienteRepository.existsByIdAndClinica(clienteId, clinica)) {
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
    public ClienteHistoricoResponseDTO historicoCliente(Clinica clinica, UUID id) {
        ClienteResponseDTO perfil = buscarPorId(clinica, id);

        List<AtendimentoResponseDTO> atendimentos = atendimentoRepository.findByCliente_Id(id).stream()
                .sorted(Comparator.comparing(Atendimento::getDataAtendimento).reversed())
                .map(atendimento -> atendimentoService.buscarPorId(clinica, atendimento.getId()))
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
    public List<ClienteResponseDTO> aniversariantes(Clinica clinica, Integer mes) {
        int mesConsulta = mes != null ? mes : LocalDate.now().getMonthValue();
        if (mesConsulta < 1 || mesConsulta > 12) {
            throw new IllegalArgumentException("Mês deve estar entre 1 e 12");
        }
        return clienteRepository.findByDataNascimentoMesAndClinica(mesConsulta, clinica).stream()
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
                statusOuProspect(cliente.getStatus()),
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
                statusOuProspect(cliente.getStatus()),
                cliente.getOrigem(),
                cliente.getCanalPreferido(),
                cliente.getPreferencias(),
                cliente.getObservacoes());
    }

    private ClienteStatus statusOuProspect(ClienteStatus status) {
        return status != null ? status : ClienteStatus.PROSPECT;
    }

}