package com.cliniva.agenda;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cliniva.agenda.dtos.AgendaItemDTO;
import com.cliniva.agenda.dtos.DisponibilidadeDiaDTO;
import com.cliniva.agenda.dtos.HorarioRequestDTO;
import com.cliniva.agenda.dtos.HorarioResponseDTO;
import com.cliniva.agenda.model.HorarioAtendimento;
import com.cliniva.agenda.model.HorarioAtendimentoId;
import com.cliniva.agenda.repository.HorarioAtendimentoRepository;
import com.cliniva.atendimento.enums.StatusAtendimento;
import com.cliniva.atendimento.model.Atendimento;
import com.cliniva.atendimento.model.AtendimentoServico;
import com.cliniva.atendimento.repository.AtendimentoRepository;
import com.cliniva.atendimento.repository.AtendimentoServicoRepository;
import com.cliniva.exception.HorarioIndisponivelException;
import com.cliniva.exception.RecursoNaoEncontradoException;
import com.cliniva.servico.Servico;
import com.cliniva.servico.ServicoRepository;
import com.cliniva.tenancy.Clinica;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AgendaService {

    public static final ZoneId ZONA_BRASIL = ZoneId.of("America/Sao_Paulo");

    private static final int PASSO_MINUTOS = 30;

    private final AtendimentoRepository atendimentoRepository;
    private final AtendimentoServicoRepository atendimentoServicoRepository;
    private final ServicoRepository servicoRepository;
    private final HorarioAtendimentoRepository horarioRepository;

    @Transactional(readOnly = true)
    public List<AgendaItemDTO> listarDia(Clinica clinica, LocalDate data) {
        LocalDateTime inicio = data.atStartOfDay();
        LocalDateTime fim = data.plusDays(1).atStartOfDay();

        return atendimentoRepository.findByClinicaAndDataAtendimentoBetween(clinica, inicio, fim).stream()
                .sorted(Comparator.comparing(Atendimento::getDataAtendimento))
                .map(this::toAgendaItemDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public DisponibilidadeDiaDTO disponibilidadeDia(Clinica clinica, LocalDate data, UUID servicoId) {
        Servico servico = servicoRepository.findByIdAndClinica(servicoId, clinica)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Serviço não encontrado"));

        Optional<HorarioAtendimento> horario = horarioDoDia(clinica, data);
        if (horario.isEmpty()) {
            return new DisponibilidadeDiaDTO(data, servico.getDuracaoMinutos(), List.of());
        }

        List<Atendimento> ocupados = atendimentosDoDia(clinica, data, StatusAtendimento.AGENDADO);
        List<LocalTime> livres = new ArrayList<>();

        LocalTime inicio = horario.get().getAbertura();
        LocalTime fechamento = horario.get().getFechamento();
        LocalDate hoje = LocalDate.now(ZONA_BRASIL);

        while (!inicio.plusMinutes(servico.getDuracaoMinutos()).isAfter(fechamento)) {
            LocalDateTime candidato = data.atTime(inicio);
            if (data.isAfter(hoje) || (data.equals(hoje) && !inicio.isBefore(LocalTime.now(ZONA_BRASIL)))) {
                if (!estaOcupado(candidato, servico.getDuracaoMinutos(), ocupados)) {
                    livres.add(inicio);
                }
            }
            inicio = inicio.plusMinutes(PASSO_MINUTOS);
        }

        return new DisponibilidadeDiaDTO(data, servico.getDuracaoMinutos(), livres);
    }

    @Transactional
    public void validarDisponibilidade(Clinica clinica, LocalDateTime inicio, int duracaoMinutos,
            UUID atendimentoExcecaoId) {
        LocalDate data = inicio.toLocalDate();
        Optional<HorarioAtendimento> horario = horarioDoDia(clinica, data);
        if (horario.isEmpty()) {
            throw new HorarioIndisponivelException("Clínica não abre neste dia");
        }

        if (inicio.toLocalTime().isBefore(horario.get().getAbertura())
                || inicio.plusMinutes(duracaoMinutos).toLocalTime().isAfter(horario.get().getFechamento())) {
            throw new HorarioIndisponivelException("Fora do horário de funcionamento");
        }

        if (data.equals(LocalDate.now(ZONA_BRASIL)) && inicio.isBefore(LocalDateTime.now(ZONA_BRASIL))) {
            throw new HorarioIndisponivelException("Horário escolhido já passou");
        }

        List<Atendimento> ocupados = atendimentoRepository
                .findByClinicaAndDataAtendimentoBetween(clinica, data.atStartOfDay(),
                        data.plusDays(1).atStartOfDay()).stream()
                .filter(a -> a.getStatus() != StatusAtendimento.CANCELADO)
                .filter(a -> atendimentoExcecaoId == null || !a.getId().equals(atendimentoExcecaoId))
                .toList();

        if (estaOcupado(inicio, duracaoMinutos, ocupados)) {
            throw new HorarioIndisponivelException("Horário já ocupado");
        }
    }

    @Transactional(readOnly = true)
    public List<HorarioResponseDTO> listarHorarios(Clinica clinica) {
        return horarioRepository.findByClinicaOrderByIdDiaSemanaAsc(clinica).stream()
                .map(this::toHorarioResponseDTO)
                .toList();
    }

    @Transactional
    public List<HorarioResponseDTO> atualizarHorarios(Clinica clinica, List<HorarioRequestDTO> horarios) {
        for (HorarioRequestDTO request : horarios) {
            if (!request.abertura().isBefore(request.fechamento())) {
                throw new IllegalArgumentException("Abertura deve ser antes do fechamento");
            }
            HorarioAtendimentoId id = new HorarioAtendimentoId(clinica.getId(), request.diaSemana());
            HorarioAtendimento horario = horarioRepository.findById(id).orElseGet(() -> {
                HorarioAtendimento novo = new HorarioAtendimento();
                novo.setId(id);
                novo.setClinica(clinica);
                return novo;
            });
            horario.setAbertura(request.abertura());
            horario.setFechamento(request.fechamento());
            horario.setAtivo(request.ativo());
            horarioRepository.save(horario);
        }
        return listarHorarios(clinica);
    }

    public void semearPadrao(Clinica clinica) {
        for (int dia = 1; dia <= 6; dia++) {
            HorarioAtendimentoId id = new HorarioAtendimentoId(clinica.getId(), dia);
            HorarioAtendimento horario = new HorarioAtendimento();
            horario.setId(id);
            horario.setClinica(clinica);
            horario.setAbertura(LocalTime.of(8, 0));
            horario.setFechamento(LocalTime.of(18, 0));
            horario.setAtivo(true);
            horarioRepository.save(horario);
        }
    }

    private Optional<HorarioAtendimento> horarioDoDia(Clinica clinica, LocalDate data) {
        return horarioRepository.findByClinicaOrderByIdDiaSemanaAsc(clinica).stream()
                .filter(h -> h.isAtivo() && h.getId().getDiaSemana().equals(data.getDayOfWeek().getValue()))
                .findFirst();
    }

    private List<Atendimento> atendimentosDoDia(Clinica clinica, LocalDate data, StatusAtendimento status) {
        return atendimentoRepository.findByClinicaAndDataAtendimentoBetween(clinica, data.atStartOfDay(),
                data.plusDays(1).atStartOfDay()).stream()
                .filter(a -> a.getStatus() == status)
                .toList();
    }

    private boolean estaOcupado(LocalDateTime inicio, int duracaoMinutos, List<Atendimento> ocupados) {
        LocalDateTime fim = inicio.plusMinutes(duracaoMinutos);
        for (Atendimento outro : ocupados) {
            LocalDateTime outroFim = outro.getDataAtendimento().plusMinutes(outro.getDuracaoMinutos());
            if (inicio.isBefore(outroFim) && outro.getDataAtendimento().isBefore(fim)) {
                return true;
            }
        }
        return false;
    }

    private AgendaItemDTO toAgendaItemDTO(Atendimento atendimento) {
        List<AtendimentoServico> servicos = atendimentoServicoRepository.findByAtendimento(atendimento);
        List<String> nomes = servicos.stream().map(s -> s.getServico().getNome()).toList();
        BigDecimal valorTotal = servicos.stream()
                .map(AtendimentoServico::getValorCobrado)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        UUID clienteId = atendimento.getCliente() != null ? atendimento.getCliente().getId() : null;
        String clienteNome = atendimento.getCliente() != null ? atendimento.getCliente().getNome() : "";
        String clienteTelefone = atendimento.getCliente() != null ? atendimento.getCliente().getTelefone() : "";

        return new AgendaItemDTO(
                atendimento.getId(),
                clienteId,
                clienteNome,
                clienteTelefone,
                atendimento.getDataAtendimento(),
                atendimento.getDataAtendimento().plusMinutes(atendimento.getDuracaoMinutos()),
                atendimento.getDuracaoMinutos(),
                atendimento.getStatus(),
                valorTotal,
                nomes);
    }

    private HorarioResponseDTO toHorarioResponseDTO(HorarioAtendimento horario) {
        return new HorarioResponseDTO(
                horario.getId().getDiaSemana(),
                horario.getAbertura(),
                horario.getFechamento(),
                horario.isAtivo());
    }
}