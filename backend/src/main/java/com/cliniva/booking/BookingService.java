package com.cliniva.booking;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cliniva.agenda.AgendaService;
import com.cliniva.agenda.dtos.DisponibilidadeDiaDTO;
import com.cliniva.atendimento.AtendimentoService;
import com.cliniva.atendimento.dtos.CreateAtendimentoRequestDTO;
import com.cliniva.atendimento.dtos.CreateAtendimentoRequestDTO.ServicoSelecionadoDTO;
import com.cliniva.atendimento.dtos.CreateAtendimentoResponseDTO;
import com.cliniva.booking.dtos.BookingRequestDTO;
import com.cliniva.booking.dtos.BookingResponseDTO;
import com.cliniva.booking.dtos.ServicoPublicoDTO;
import com.cliniva.cliente.Cliente;
import com.cliniva.cliente.ClienteRepository;
import com.cliniva.cliente.enums.ClienteStatus;
import com.cliniva.cliente.enums.OrigemCliente;
import com.cliniva.exception.HorarioIndisponivelException;
import com.cliniva.exception.RecursoNaoEncontradoException;
import com.cliniva.servico.Servico;
import com.cliniva.servico.ServicoRepository;
import com.cliniva.tenancy.Clinica;
import com.cliniva.tenancy.ClinicaRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BookingService {

    private static final int JANELA_DIAS = 30;

    private final ClinicaRepository clinicaRepository;
    private final ClienteRepository clienteRepository;
    private final ServicoRepository servicoRepository;
    private final AgendaService agendaService;
    private final AtendimentoService atendimentoService;

    @Transactional(readOnly = true)
    public List<ServicoPublicoDTO> listarServicos(String slug) {
        Clinica clinica = buscarClinica(slug);
        return servicoRepository.findByClinica(clinica).stream()
                .map(servico -> new ServicoPublicoDTO(servico.getId(), servico.getNome(),
                        servico.getDescricao(), servico.getValor(), servico.getDuracaoMinutos(),
                        clinica.getNome()))
                .toList();
    }

    @Transactional(readOnly = true)
    public DisponibilidadeDiaDTO disponibilidade(String slug, LocalDate data, UUID servicoId) {
        Clinica clinica = buscarClinica(slug);
        validarJanela(data);
        return agendaService.disponibilidadeDia(clinica, data, servicoId);
    }

    @Transactional
    public BookingResponseDTO agendar(String slug, BookingRequestDTO request) {
        Clinica clinica = buscarClinica(slug);
        Servico servico = servicoRepository.findByIdAndClinica(request.servicoId(), clinica)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Serviço não encontrado"));

        validarJanela(request.dataHora().toLocalDate());
        agendaService.validarDisponibilidade(clinica, request.dataHora(), servico.getDuracaoMinutos(), null);

        Cliente cliente = clienteRepository.findByTelefoneAndClinica(request.telefone(), clinica)
                .orElseGet(() -> {
                    Cliente novo = new Cliente();
                    novo.setClinica(clinica);
                    novo.setNome(request.nome().trim());
                    novo.setEmail(request.email());
                    novo.setTelefone(request.telefone());
                    novo.setOrigem(OrigemCliente.ONLINE);
                    novo.setStatus(ClienteStatus.PROSPECT);
                    return clienteRepository.save(novo);
                });

        CreateAtendimentoRequestDTO atendimentoRequest = new CreateAtendimentoRequestDTO(
                cliente.getId(),
                request.dataHora(),
                List.of(new ServicoSelecionadoDTO(request.servicoId(), List.of())));
        CreateAtendimentoResponseDTO criado = atendimentoService.createAtendimento(clinica, atendimentoRequest);

        return new BookingResponseDTO(
                criado.id(),
                criado.dataAtendimento(),
                criado.duracaoMinutos(),
                servico.getNome(),
                clinica.getNome(),
                cliente.getNome());
    }

    private Clinica buscarClinica(String slug) {
        return clinicaRepository.findBySlug(slug)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Clínica não encontrada"));
    }

    private void validarJanela(LocalDate data) {
        LocalDate hoje = LocalDate.now(AgendaService.ZONA_BRASIL);
        if (data.isBefore(hoje) || data.isAfter(hoje.plusDays(JANELA_DIAS))) {
            throw new HorarioIndisponivelException(
                    "Só é possível agendar entre hoje e os próximos " + JANELA_DIAS + " dias");
        }
    }
}