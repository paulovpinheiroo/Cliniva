package com.cliniva.resumo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cliniva.atendimento.enums.StatusAtendimento;
import com.cliniva.atendimento.model.Atendimento;
import com.cliniva.atendimento.repository.AtendimentoRepository;
import com.cliniva.atendimento.repository.AtendimentoServicoRepository;
import com.cliniva.cliente.Cliente;
import com.cliniva.cliente.ClienteRepository;
import com.cliniva.item.Item;
import com.cliniva.item.ItemRepository;
import com.cliniva.tenancy.Clinica;

@ExtendWith(MockitoExtension.class)
class ResumoAgregadorTest {

    private static final ZoneId ZONA = ZoneId.of("America/Sao_Paulo");
    private static final LocalDate HOJE = LocalDate.of(2026, 9, 18);

    @Mock
    private AtendimentoRepository atendimentoRepository;
    @Mock
    private AtendimentoServicoRepository atendimentoServicoRepository;
    @Mock
    private ClienteRepository clienteRepository;
    @Mock
    private ItemRepository itemRepository;

    @InjectMocks
    private ResumoAgregador agregador;

    @Test
    void deveAgregarNumerosDoDiaIgnorandoCancelados() {
        Clinica clinica = clinica("Clínica A", UUID.randomUUID());
        Atendimento atendido = atendimento(clinica, "2026-09-18T10:00", StatusAtendimento.CONCLUIDO,
                cliente("1").getId());
        Atendimento cancelado = atendimento(clinica, "2026-09-18T09:00", StatusAtendimento.CANCELADO,
                cliente("2").getId());

        when(atendimentoRepository.findByClinicaAndDataAtendimentoBetween(eq(clinica), any(), any()))
                .thenReturn(List.of(atendido, cancelado));
        when(atendimentoServicoRepository.somarValorCobradoPorStatusEPeriodo(eq(clinica),
                eq(StatusAtendimento.AGENDADO), any(), any())).thenReturn(new BigDecimal("150.00"));
        when(atendimentoServicoRepository.somarValorCobradoPorStatusEPeriodo(eq(clinica),
                eq(StatusAtendimento.CONCLUIDO), any(), any())).thenReturn(new BigDecimal("90.00"));
        when(atendimentoRepository.findByClinicaAndDataAtendimentoBefore(eq(clinica), any()))
                .thenReturn(List.of(atendimento(clinica, "2026-08-10T10:00", StatusAtendimento.CONCLUIDO,
                        cliente("1").getId())));
        when(atendimentoRepository.findByClinicaAndDataAtendimentoBetween(eq(clinica),
                eq(HOJE.withDayOfMonth(1).atStartOfDay(ZONA).toLocalDateTime()), any()))
                .thenReturn(List.of(atendido));
        when(clienteRepository.findAniversariantesHojeByClinica(18, 9, clinica))
                .thenReturn(List.of(cliente("3"), cliente("4")));
        when(itemRepository.findByClinicaAndQuantidadeEmEstoqueLessThan(eq(clinica), any()))
                .thenReturn(List.of(item("Sérum Vitamina C"), item("Batom")));

        ResumoContexto contexto = agregador.agregar(clinica, HOJE, ZONA);

        assertThat(contexto.atendimentosHoje()).isEqualTo(1);
        assertThat(contexto.receitaPrevista()).isEqualByComparingTo("150.00");
        assertThat(contexto.receitaRealizada()).isEqualByComparingTo("90.00");
        assertThat(contexto.novosMes()).isZero();
        assertThat(contexto.recorrentesMes()).isEqualTo(1);
        assertThat(contexto.aniversariantesHoje()).isEqualTo(2);
        assertThat(contexto.itensAbaixoMinimo())
                .containsExactly("Batom", "Sérum Vitamina C");
    }

    @Test
    void deveDividirNovasERecorrentesNoMes() {
        Clinica clinica = clinica("Clínica A", UUID.randomUUID());
        UUID antiga = cliente("1").getId();
        UUID nova = cliente("2").getId();

        when(atendimentoRepository.findByClinicaAndDataAtendimentoBefore(eq(clinica), any()))
                .thenReturn(List.of(atendimento(clinica, "2026-08-10T10:00",
                        StatusAtendimento.CONCLUIDO, antiga)));
        when(atendimentoRepository.findByClinicaAndDataAtendimentoBetween(eq(clinica), any(), any()))
                .thenReturn(List.of());
        when(atendimentoRepository.findByClinicaAndDataAtendimentoBetween(eq(clinica),
                eq(HOJE.withDayOfMonth(1).atStartOfDay(ZONA).toLocalDateTime()), any()))
                .thenReturn(List.of(
                        atendimento(clinica, "2026-09-05T10:00", StatusAtendimento.CONCLUIDO, antiga),
                        atendimento(clinica, "2026-09-06T10:00", StatusAtendimento.CONCLUIDO, nova),
                        atendimento(clinica, "2026-09-07T10:00", StatusAtendimento.CONCLUIDO, nova)));
        when(atendimentoServicoRepository.somarValorCobradoPorStatusEPeriodo(eq(clinica),
                eq(StatusAtendimento.AGENDADO), any(), any())).thenReturn(BigDecimal.ZERO);
        when(atendimentoServicoRepository.somarValorCobradoPorStatusEPeriodo(eq(clinica),
                eq(StatusAtendimento.CONCLUIDO), any(), any())).thenReturn(BigDecimal.ZERO);
        when(clienteRepository.findAniversariantesHojeByClinica(18, 9, clinica))
                .thenReturn(List.of());
        when(itemRepository.findByClinicaAndQuantidadeEmEstoqueLessThan(eq(clinica), any()))
                .thenReturn(List.of());

        ResumoContexto contexto = agregador.agregar(clinica, HOJE, ZONA);

        assertThat(contexto.novosMes()).isEqualTo(1);
        assertThat(contexto.recorrentesMes()).isEqualTo(1);
    }

    private static Clinica clinica(String nome, UUID id) {
        Clinica clinica = new Clinica();
        clinica.setId(id);
        clinica.setNome(nome);
        return clinica;
    }

    private static Cliente cliente(String id) {
        Cliente cliente = new Cliente();
        cliente.setId(UUID.fromString("00000000-0000-0000-0000-00000000000" + id));
        return cliente;
    }

    private static Atendimento atendimento(Clinica clinica, String data, StatusAtendimento status, UUID clienteId) {
        Atendimento atendimento = new Atendimento();
        atendimento.setClinica(clinica);
        atendimento.setDataAtendimento(LocalDateTime.parse(data));
        atendimento.setStatus(status);
        Cliente cliente = new Cliente();
        cliente.setId(clienteId);
        atendimento.setCliente(cliente);
        return atendimento;
    }

    private static Item item(String nome) {
        Item item = new Item();
        item.setNome(nome);
        return item;
    }
}