package com.cliniva.agenda;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.cliniva.agenda.model.HorarioAtendimento;
import com.cliniva.agenda.model.HorarioAtendimentoId;
import com.cliniva.agenda.repository.HorarioAtendimentoRepository;
import com.cliniva.agenda.dtos.HorarioRequestDTO;
import com.cliniva.atendimento.enums.StatusAtendimento;
import com.cliniva.atendimento.model.Atendimento;
import com.cliniva.atendimento.model.AtendimentoServico;
import com.cliniva.atendimento.repository.AtendimentoRepository;
import com.cliniva.atendimento.repository.AtendimentoServicoRepository;
import com.cliniva.cliente.Cliente;
import com.cliniva.exception.HorarioIndisponivelException;
import com.cliniva.servico.Servico;
import com.cliniva.servico.ServicoRepository;
import com.cliniva.tenancy.Clinica;

@ExtendWith(MockitoExtension.class)
class AgendaServiceTest {

    private static final Clinica CLINICA = clinica();

    private static final LocalDate SEGUNDA = LocalDate.of(2026, 9, 28);

    @Mock
    private AtendimentoRepository atendimentoRepository;
    @Mock
    private AtendimentoServicoRepository atendimentoServicoRepository;
    @Mock
    private ServicoRepository servicoRepository;
    @Mock
    private HorarioAtendimentoRepository horarioRepository;

    @InjectMocks
    private AgendaService agendaService;

    private static Clinica clinica() {
        Clinica clinica = new Clinica();
        ReflectionTestUtils.setField(clinica, "id", UUID.randomUUID());
        clinica.setNome("Clínica A");
        return clinica;
    }

    private Servico servico(String nome, String valor, int duracao) {
        Servico servico = new Servico();
        servico.setClinica(CLINICA);
        servico.setNome(nome);
        servico.setValor(new BigDecimal(valor));
        servico.setDuracaoMinutos(duracao);
        return servico;
    }

    private HorarioAtendimento horario(int dia, String abertura, String fechamento, boolean ativo) {
        HorarioAtendimento h = new HorarioAtendimento();
        h.setId(new HorarioAtendimentoId(CLINICA.getId(), dia));
        h.setClinica(CLINICA);
        h.setAbertura(LocalTime.parse(abertura));
        h.setFechamento(LocalTime.parse(fechamento));
        h.setAtivo(ativo);
        return h;
    }

    private Atendimento atendimento(UUID id, LocalDateTime inicio, int duracao, StatusAtendimento status) {
        Atendimento a = new Atendimento();
        ReflectionTestUtils.setField(a, "id", id);
        a.setClinica(CLINICA);
        a.setDataAtendimento(inicio);
        a.setDuracaoMinutos(duracao);
        a.setStatus(status);

        Cliente cliente = new Cliente();
        ReflectionTestUtils.setField(cliente, "id", UUID.randomUUID());
        cliente.setNome("Maria");
        cliente.setTelefone("11999999999");
        a.setCliente(cliente);
        return a;
    }

    private AtendimentoServico atendimentoServico(Atendimento a, Servico s) {
        AtendimentoServico as = new AtendimentoServico();
        as.setAtendimento(a);
        as.setServico(s);
        as.setValorCobrado(s.getValor());
        return as;
    }

    @Test
    void deveListarDiaOrdenadoPorHoraComDuracaoEValorTotal() {
        Servico limpeza = servico("Limpeza de Pele", "120.00", 45);
        Servico massagem = servico("Massagem", "90.00", 60);

        Atendimento tarde = atendimento(UUID.randomUUID(), SEGUNDA.atTime(14, 0), 60, StatusAtendimento.AGENDADO);
        Atendimento manha = atendimento(UUID.randomUUID(), SEGUNDA.atTime(9, 0), 45, StatusAtendimento.AGENDADO);

        when(atendimentoRepository.findByClinicaAndDataAtendimentoBetween(any(), any(), any()))
                .thenReturn(List.of(tarde, manha));
        when(atendimentoServicoRepository.findByAtendimento(manha))
                .thenReturn(List.of(atendimentoServico(manha, limpeza)));
        when(atendimentoServicoRepository.findByAtendimento(tarde))
                .thenReturn(List.of(atendimentoServico(tarde, massagem)));

        var itens = agendaService.listarDia(CLINICA, SEGUNDA);

        assertThat(itens).hasSize(2);
        assertThat(itens.get(0).clienteNome()).isEqualTo("Maria");
        assertThat(itens.get(0).inicio()).isEqualTo(SEGUNDA.atTime(9, 0));
        assertThat(itens.get(0).fim()).isEqualTo(SEGUNDA.atTime(9, 45));
        assertThat(itens.get(0).duracaoMinutos()).isEqualTo(45);
        assertThat(itens.get(0).valorTotal()).isEqualByComparingTo("120.00");
        assertThat(itens.get(1).inicio()).isEqualTo(SEGUNDA.atTime(14, 0));
    }

    @Test
    void deveGerarHorariosLivresDentroDoExpediente() {
        Servico servico = servico("Limpeza", "100.00", 30);
        when(servicoRepository.findByIdAndClinica(any(), any())).thenReturn(Optional.of(servico));
        when(horarioRepository.findByClinicaOrderByIdDiaSemanaAsc(CLINICA))
                .thenReturn(List.of(horario(1, "08:00", "10:00", true)));
        when(atendimentoRepository.findByClinicaAndDataAtendimentoBetween(any(), any(), any()))
                .thenReturn(List.of());

        var disponibilidade = agendaService.disponibilidadeDia(CLINICA, SEGUNDA, UUID.randomUUID());

        assertThat(disponibilidade.horarios()).containsExactly(
                LocalTime.of(8, 0), LocalTime.of(8, 30), LocalTime.of(9, 0), LocalTime.of(9, 30));
    }

    @Test
    void deveIgnorarHorarioJaOcupado() {
        Servico servico = servico("Limpeza", "100.00", 30);
        Atendimento ocupado = atendimento(UUID.randomUUID(), SEGUNDA.atTime(8, 30), 30,
                StatusAtendimento.AGENDADO);

        when(servicoRepository.findByIdAndClinica(any(), any())).thenReturn(Optional.of(servico));
        when(horarioRepository.findByClinicaOrderByIdDiaSemanaAsc(CLINICA))
                .thenReturn(List.of(horario(1, "08:00", "10:00", true)));
        when(atendimentoRepository.findByClinicaAndDataAtendimentoBetween(any(), any(), any()))
                .thenReturn(List.of(ocupado));

        var disponibilidade = agendaService.disponibilidadeDia(CLINICA, SEGUNDA, UUID.randomUUID());

        assertThat(disponibilidade.horarios()).containsExactly(
                LocalTime.of(8, 0), LocalTime.of(9, 0), LocalTime.of(9, 30));
    }

    @Test
    void deveRetornarListaVaziaQuandoClinicaFechadaNoDia() {
        Servico servico = servico("Limpeza", "100.00", 30);
        when(servicoRepository.findByIdAndClinica(any(), any())).thenReturn(Optional.of(servico));
        when(horarioRepository.findByClinicaOrderByIdDiaSemanaAsc(CLINICA)).thenReturn(List.of());

        var disponibilidade = agendaService.disponibilidadeDia(CLINICA, SEGUNDA, UUID.randomUUID());

        assertThat(disponibilidade.horarios()).isEmpty();
    }

    @Test
    void naoDeveValidarHorarioQueConflitaComOutroAtendimento() {
        Atendimento ocupado = atendimento(UUID.randomUUID(), SEGUNDA.atTime(9, 0), 30,
                StatusAtendimento.AGENDADO);
        when(horarioRepository.findByClinicaOrderByIdDiaSemanaAsc(CLINICA))
                .thenReturn(List.of(horario(1, "08:00", "18:00", true)));
        when(atendimentoRepository.findByClinicaAndDataAtendimentoBetween(any(), any(), any()))
                .thenReturn(List.of(ocupado));

        assertThatThrownBy(() -> agendaService.validarDisponibilidade(CLINICA, SEGUNDA.atTime(9, 0), 30, null))
                .isInstanceOf(HorarioIndisponivelException.class)
                .hasMessageContaining("ocupado");
    }

    @Test
    void naoDeveValidarHorarioForaDoExpediente() {
        when(horarioRepository.findByClinicaOrderByIdDiaSemanaAsc(CLINICA))
                .thenReturn(List.of(horario(1, "08:00", "18:00", true)));

        assertThatThrownBy(() -> agendaService.validarDisponibilidade(CLINICA, SEGUNDA.atTime(19, 0), 45, null))
                .isInstanceOf(HorarioIndisponivelException.class)
                .hasMessageContaining("funcionamento");
    }

    @Test
    void naoDeveValidarQuandoClinicaNaoAbreNoDia() {
        when(horarioRepository.findByClinicaOrderByIdDiaSemanaAsc(CLINICA)).thenReturn(List.of());
        LocalDate domingo = SEGUNDA.plusDays(6);

        assertThatThrownBy(() -> agendaService.validarDisponibilidade(CLINICA, domingo.atTime(10, 0), 30, null))
                .isInstanceOf(HorarioIndisponivelException.class)
                .hasMessageContaining("não abre");
    }

    @Test
    void devePermitirHorarioLivre() {
        when(horarioRepository.findByClinicaOrderByIdDiaSemanaAsc(CLINICA))
                .thenReturn(List.of(horario(1, "08:00", "18:00", true)));
        when(atendimentoRepository.findByClinicaAndDataAtendimentoBetween(any(), any(), any()))
                .thenReturn(List.of());

        agendaService.validarDisponibilidade(CLINICA, SEGUNDA.atTime(10, 0), 45, null);
    }

    @Test
    void deveAtualizarHorariosComUpsert() {
        HorarioAtendimentoId idExistente = new HorarioAtendimentoId(CLINICA.getId(), 1);
        HorarioAtendimento existente = horario(1, "08:00", "18:00", true);
        when(horarioRepository.findById(idExistente)).thenReturn(Optional.of(existente));
        when(horarioRepository.findById(new HorarioAtendimentoId(CLINICA.getId(), 2)))
                .thenReturn(Optional.empty());
        when(horarioRepository.findByClinicaOrderByIdDiaSemanaAsc(CLINICA))
                .thenReturn(List.of(existente));

        agendaService.atualizarHorarios(CLINICA, List.of(
                new HorarioRequestDTO(1, LocalTime.of(9, 0), LocalTime.of(17, 0), true),
                new HorarioRequestDTO(2, LocalTime.of(8, 0), LocalTime.of(12, 0), true)));

        assertThat(existente.getAbertura()).isEqualTo(LocalTime.of(9, 0));
        verify(horarioRepository, times(2)).save(any(HorarioAtendimento.class));
    }

    @Test
    void deveSemearPadraoDeSegundaASabado() {
        agendaService.semearPadrao(CLINICA);

        verify(horarioRepository, times(6)).save(any(HorarioAtendimento.class));
    }
}