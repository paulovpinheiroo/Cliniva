package com.cliniva.agenda;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
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
import com.cliniva.tenancy.ClinicaRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AgendaService {

    /** Teto de duração de um atendimento (1 dia) — evita wrap de LocalTime e abuse. */
    public static final int DURACAO_MAXIMA_MINUTOS = 1440;

    /** Janela de agendamento permitida para qualquer fluxo (público e interno). */
    public static final int JANELA_DIAS = 30;

    private static final int PASSO_MINUTOS = 30;

    private final AtendimentoRepository atendimentoRepository;
    private final AtendimentoServicoRepository atendimentoServicoRepository;
    private final ServicoRepository servicoRepository;
    private final HorarioAtendimentoRepository horarioRepository;
    private final ClinicaRepository clinicaRepository;
    private final Clock clock;

    @Transactional(readOnly = true)
    public List<AgendaItemDTO> listarDia(Clinica clinica, LocalDate data) {
        LocalDateTime inicio = data.atStartOfDay();
        LocalDateTime fim = data.plusDays(1).atStartOfDay();

        return atendimentoRepository.findPorIntervalo(clinica, inicio, fim).stream()
                .sorted(Comparator.comparing(Atendimento::getDataAtendimento))
                .map(this::toAgendaItemDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public DisponibilidadeDiaDTO disponibilidadeDia(Clinica clinica, LocalDate data, UUID servicoId) {
        Servico servico = servicoRepository.findByIdAndClinica(servicoId, clinica)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Serviço não encontrado"));

        int duracao = duracaoValida(servico.getDuracaoMinutos());

        // Regra de janela: nunca sugerir horário que não pode ser agendado.
        LocalDate hoje = LocalDate.now(clock);
        if (data.isBefore(hoje) || data.isAfter(hoje.plusDays(JANELA_DIAS))) {
            return new DisponibilidadeDiaDTO(data, duracao, List.of());
        }

        Optional<HorarioAtendimento> horario = horarioDoDia(clinica, data);
        if (horario.isEmpty()) {
            return new DisponibilidadeDiaDTO(data, duracao, List.of());
        }

        List<Atendimento> ocupados = atendimentosBloqueantes(clinica, data, null);
        List<LocalTime> livres = new ArrayList<>();

        LocalDateTime abertura = data.atTime(horario.get().getAbertura());
        LocalDateTime fechamento = data.atTime(horario.get().getFechamento());
        LocalDateTime agora = LocalDateTime.now(clock);

        LocalDateTime candidato = abertura;
        while (true) {
            LocalDateTime fim = candidato.plusMinutes(duracao);
            if (fim.isAfter(fechamento)) {
                break;
            }
            if (!candidato.isBefore(agora) && !estaOcupado(candidato, duracao, ocupados)) {
                livres.add(candidato.toLocalTime());
            }
            candidato = candidato.plusMinutes(PASSO_MINUTOS);
        }

        return new DisponibilidadeDiaDTO(data, duracao, livres);
    }

    @Transactional
    public void validarDisponibilidade(Clinica clinica, LocalDateTime inicio, int duracaoMinutos,
            UUID atendimentoExcecaoId) {
        int duracao = duracaoValida(duracaoMinutos);

        // Serializa todas as escritas de atendimento desta clínica (create, update e
        // consumo de estoque) evitando double booking e estoque negativo.
        clinicaRepository.findByIdParaUpdate(clinica.getId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Clínica não encontrada"));

        LocalDate data = inicio.toLocalDate();
        LocalDate hoje = LocalDate.now(clock);

        if (data.isBefore(hoje)) {
            throw new HorarioIndisponivelException("Não é possível agendar em uma data passada");
        }
        if (data.isAfter(hoje.plusDays(JANELA_DIAS))) {
            throw new HorarioIndisponivelException(
                    "Só é possível agendar entre hoje e os próximos " + JANELA_DIAS + " dias");
        }

        Optional<HorarioAtendimento> horario = horarioDoDia(clinica, data);
        if (horario.isEmpty()) {
            throw new HorarioIndisponivelException("Clínica não abre neste dia");
        }

        LocalDateTime abertura = data.atTime(horario.get().getAbertura());
        LocalDateTime fechamento = data.atTime(horario.get().getFechamento());
        LocalDateTime fim = inicio.plusMinutes(duracao);

        if (inicio.isBefore(abertura) || fim.isAfter(fechamento)) {
            throw new HorarioIndisponivelException("Fora do horário de funcionamento");
        }

        if (inicio.isBefore(LocalDateTime.now(clock))) {
            throw new HorarioIndisponivelException("Horário escolhido já passou");
        }

        List<Atendimento> ocupados = atendimentosBloqueantes(clinica, data, atendimentoExcecaoId);
        if (estaOcupado(inicio, duracao, ocupados)) {
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
        if (horarios == null || horarios.isEmpty() || horarios.size() > 7) {
            throw new IllegalArgumentException("Informe entre 1 e 7 dias de expediente");
        }
        Set<Integer> dias = new HashSet<>();
        for (HorarioRequestDTO request : horarios) {
            if (request.diaSemana() == null) {
                throw new IllegalArgumentException("Dia da semana é obrigatório");
            }
            if (!dias.add(request.diaSemana())) {
                throw new IllegalArgumentException("Dia da semana duplicado: " + request.diaSemana());
            }
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
            horario.setAtivo(request.ativoOuPadrao());
            horarioRepository.save(horario);
        }
        return listarHorarios(clinica);
    }

    /** Expediente padrão de segunda a sábado, 08:00–18:00. */
    @Transactional
    public void semearPadrao(Clinica clinica) {
        for (int dia = 1; dia <= 6; dia++) {
            HorarioAtendimentoId id = new HorarioAtendimentoId(clinica.getId(), dia);
            if (horarioRepository.existsById(id)) {
                continue;
            }
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

    /**
     * Atendimentos que bloqueiam o dia: mesma clínica, status diferente de
     * CANCELADO e início na janela [ontem 00:00, amanhã 00:00) — a janela
     * estendida captura atendimentos que começam no dia anterior e terminam no
     * dia corrente. A sobreposição real é checada em memória por intervalo.
     */
    private List<Atendimento> atendimentosBloqueantes(Clinica clinica, LocalDate data, UUID excecaoId) {
        LocalDateTime inicio = data.minusDays(1).atStartOfDay();
        LocalDateTime fim = data.plusDays(1).atStartOfDay();
        return atendimentoRepository.findPorIntervalo(clinica, inicio, fim).stream()
                .filter(a -> a.getStatus() != StatusAtendimento.CANCELADO)
                .filter(a -> excecaoId == null || !a.getId().equals(excecaoId))
                .toList();
    }

    private boolean estaOcupado(LocalDateTime inicio, int duracaoMinutos, List<Atendimento> ocupados) {
        LocalDateTime fim = inicio.plusMinutes(duracaoMinutos);
        for (Atendimento outro : ocupados) {
            int duracaoOutro = Math.min(outro.getDuracaoMinutos(), DURACAO_MAXIMA_MINUTOS);
            LocalDateTime outroFim = outro.getDataAtendimento().plusMinutes(duracaoOutro);
            if (inicio.isBefore(outroFim) && outro.getDataAtendimento().isBefore(fim)) {
                return true;
            }
        }
        return false;
    }

    private int duracaoValida(int duracaoMinutos) {
        if (duracaoMinutos <= 0 || duracaoMinutos > DURACAO_MAXIMA_MINUTOS) {
            throw new HorarioIndisponivelException(
                    "Duração do atendimento deve estar entre 1 e " + DURACAO_MAXIMA_MINUTOS + " minutos");
        }
        return duracaoMinutos;
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
