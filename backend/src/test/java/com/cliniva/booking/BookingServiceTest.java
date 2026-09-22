package com.cliniva.booking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.cliniva.agenda.AgendaService;
import com.cliniva.atendimento.AtendimentoService;
import com.cliniva.atendimento.dtos.CreateAtendimentoRequestDTO;
import com.cliniva.atendimento.dtos.CreateAtendimentoResponseDTO;
import com.cliniva.atendimento.dtos.CreateAtendimentoResponseDTO.ServicoRealizadoDTO;
import com.cliniva.atendimento.enums.StatusAtendimento;
import com.cliniva.booking.dtos.BookingRequestDTO;
import com.cliniva.cliente.Cliente;
import com.cliniva.cliente.ClienteRepository;
import com.cliniva.cliente.enums.OrigemCliente;
import com.cliniva.exception.HorarioIndisponivelException;
import com.cliniva.exception.RecursoNaoEncontradoException;
import com.cliniva.servico.Servico;
import com.cliniva.servico.ServicoRepository;
import com.cliniva.tenancy.Clinica;
import com.cliniva.tenancy.ClinicaRepository;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    private static final String SLUG = "clinica-a";

    private static final Clinica CLINICA = clinica();

    @Mock
    private ClinicaRepository clinicaRepository;
    @Mock
    private ClienteRepository clienteRepository;
    @Mock
    private ServicoRepository servicoRepository;
    @Mock
    private AgendaService agendaService;
    @Mock
    private AtendimentoService atendimentoService;

    @InjectMocks
    private BookingService bookingService;

    private static Clinica clinica() {
        Clinica clinica = new Clinica();
        ReflectionTestUtils.setField(clinica, "id", UUID.randomUUID());
        clinica.setNome("Clínica A");
        clinica.setSlug(SLUG);
        return clinica;
    }

    private Servico servico() {
        Servico servico = new Servico();
        ReflectionTestUtils.setField(servico, "id", UUID.randomUUID());
        servico.setClinica(CLINICA);
        servico.setNome("Limpeza de Pele");
        servico.setValor(new BigDecimal("120.00"));
        servico.setDuracaoMinutos(45);
        return servico;
    }

    private BookingRequestDTO requisicao(LocalDateTime dataHora) {
        return new BookingRequestDTO(UUID.randomUUID(), dataHora, "Maria Silva", "11999999999", "maria@email.com");
    }

    private CreateAtendimentoResponseDTO respostaCriada(UUID id, LocalDateTime dataHora) {
        return new CreateAtendimentoResponseDTO(id, UUID.randomUUID(), dataHora,
                LocalDate.now(), 45, StatusAtendimento.AGENDADO, List.<ServicoRealizadoDTO>of());
    }

    @Test
    void naoDeveAgendarParaClinicaInexistente() {
        when(clinicaRepository.findBySlug(SLUG)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingService.agendar(SLUG, requisicao(LocalDateTime.now().plusDays(1))))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void deveCriarClienteNovoComOrigemOnline() {
        Servico servico = servico();
        LocalDateTime dataHora = LocalDateTime.of(2026, 10, 5, 10, 0);

        when(clinicaRepository.findBySlug(SLUG)).thenReturn(Optional.of(CLINICA));
        when(servicoRepository.findByIdAndClinica(any(), any())).thenReturn(Optional.of(servico));
        when(clienteRepository.findByTelefoneAndClinica("11999999999", CLINICA)).thenReturn(Optional.empty());
        when(clienteRepository.save(any(Cliente.class))).thenAnswer(i -> i.getArgument(0));
        when(atendimentoService.createAtendimento(any(), any(CreateAtendimentoRequestDTO.class)))
                .thenAnswer(i -> respostaCriada(UUID.randomUUID(), dataHora));

        var resposta = bookingService.agendar(SLUG, requisicao(dataHora));

        ArgumentCaptor<Cliente> captor = ArgumentCaptor.forClass(Cliente.class);
        verify(clienteRepository).save(captor.capture());
        assertThat(captor.getValue().getOrigem()).isEqualTo(OrigemCliente.ONLINE);
        assertThat(captor.getValue().getNome()).isEqualTo("Maria Silva");

        assertThat(resposta.cliente()).isEqualTo("Maria Silva");
        assertThat(resposta.servico()).isEqualTo("Limpeza de Pele");
        assertThat(resposta.duracaoMinutos()).isEqualTo(45);
    }

    @Test
    void deveReutilizarClienteJaCadastrado() {
        Servico servico = servico();
        LocalDateTime dataHora = LocalDateTime.of(2026, 10, 5, 10, 0);
        Cliente cliente = new Cliente();
        ReflectionTestUtils.setField(cliente, "id", UUID.randomUUID());
        cliente.setNome("Maria Silva");

        when(clinicaRepository.findBySlug(SLUG)).thenReturn(Optional.of(CLINICA));
        when(servicoRepository.findByIdAndClinica(any(), any())).thenReturn(Optional.of(servico));
        when(clienteRepository.findByTelefoneAndClinica("11999999999", CLINICA))
                .thenReturn(Optional.of(cliente));
        when(atendimentoService.createAtendimento(any(), any(CreateAtendimentoRequestDTO.class)))
                .thenAnswer(i -> respostaCriada(UUID.randomUUID(), dataHora));

        bookingService.agendar(SLUG, requisicao(dataHora));

        verify(clienteRepository, never()).save(any(Cliente.class));
    }

    @Test
    void naoDeveAgendarForaDaJanelaDeTrintaDias() {
        when(clinicaRepository.findBySlug(SLUG)).thenReturn(Optional.of(CLINICA));
        when(servicoRepository.findByIdAndClinica(any(), any())).thenReturn(Optional.of(servico()));

        assertThatThrownBy(() -> bookingService.agendar(SLUG,
                requisicao(LocalDateTime.now().plusDays(40))))
                .isInstanceOf(HorarioIndisponivelException.class);

        verify(atendimentoService, never()).createAtendimento(any(), any());
    }

    @Test
    void deveRepassarIndisponibilidadeDaAgenda() {
        when(clinicaRepository.findBySlug(SLUG)).thenReturn(Optional.of(CLINICA));
        when(servicoRepository.findByIdAndClinica(any(), any())).thenReturn(Optional.of(servico()));
        doThrow(new HorarioIndisponivelException("Horário já ocupado")).when(agendaService)
                .validarDisponibilidade(any(), any(), anyInt(), any());

        assertThatThrownBy(() -> bookingService.agendar(SLUG,
                requisicao(LocalDateTime.of(2026, 10, 5, 10, 0))))
                .isInstanceOf(HorarioIndisponivelException.class)
                .hasMessageContaining("ocupado");

        verify(atendimentoService, never()).createAtendimento(any(), any());
    }

    @Test
    void deveListarServicosDaClinica() {
        when(clinicaRepository.findBySlug(SLUG)).thenReturn(Optional.of(CLINICA));
        when(servicoRepository.findByClinica(CLINICA)).thenReturn(List.of(servico()));

        var servicos = bookingService.listarServicos(SLUG);

        assertThat(servicos).hasSize(1);
        assertThat(servicos.get(0).clinica()).isEqualTo("Clínica A");
        assertThat(servicos.get(0).duracaoMinutos()).isEqualTo(45);
    }
}