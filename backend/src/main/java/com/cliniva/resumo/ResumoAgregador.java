package com.cliniva.resumo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.cliniva.atendimento.enums.StatusAtendimento;
import com.cliniva.atendimento.model.Atendimento;
import com.cliniva.atendimento.repository.AtendimentoRepository;
import com.cliniva.atendimento.repository.AtendimentoServicoRepository;
import com.cliniva.cliente.Cliente;
import com.cliniva.cliente.ClienteRepository;
import com.cliniva.item.Item;
import com.cliniva.item.ItemRepository;
import com.cliniva.tenancy.Clinica;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ResumoAgregador {

    static final BigDecimal ESTOQUE_BAIXO_LIMITE = BigDecimal.valueOf(5);

    private final AtendimentoRepository atendimentoRepository;
    private final AtendimentoServicoRepository atendimentoServicoRepository;
    private final ClienteRepository clienteRepository;
    private final ItemRepository itemRepository;

    @Transactional(readOnly = true)
    public ResumoContexto agregar(Clinica clinica, LocalDate hoje, ZoneId zona) {
        LocalDateTime inicioDia = hoje.atStartOfDay(zona).toLocalDateTime();
        LocalDateTime fimDia = hoje.atTime(LocalTime.MAX);

        long naoCanceladosHoje = atendimentoRepository
                .findByClinicaAndDataAtendimentoBetween(clinica, inicioDia, fimDia).stream()
                .filter(atendimento -> atendimento.getStatus() != StatusAtendimento.CANCELADO)
                .count();

        BigDecimal receitaPrevista = atendimentoServicoRepository
                .somarValorCobradoPorStatusEPeriodo(clinica, StatusAtendimento.AGENDADO, inicioDia, fimDia);
        BigDecimal receitaRealizada = atendimentoServicoRepository
                .somarValorCobradoPorStatusEPeriodo(clinica, StatusAtendimento.CONCLUIDO, inicioDia, fimDia);

        int[] novosERecorrentes = calcularNovosERecorrentes(clinica, hoje, zona);

        List<Cliente> aniversariantes = clienteRepository.findAniversariantesHojeByClinica(
                hoje.getDayOfMonth(), hoje.getMonthValue(), clinica);

        List<String> itensAbaixoMinimo = itemRepository
                .findByClinicaAndQuantidadeEmEstoqueLessThan(clinica, ESTOQUE_BAIXO_LIMITE).stream()
                .map(Item::getNome)
                .sorted()
                .toList();

        return new ResumoContexto(
                clinica.getNome(),
                Math.toIntExact(naoCanceladosHoje),
                receitaPrevista,
                receitaRealizada,
                novosERecorrentes[0],
                novosERecorrentes[1],
                aniversariantes.size(),
                itensAbaixoMinimo);
    }

    private int[] calcularNovosERecorrentes(Clinica clinica, LocalDate hoje, ZoneId zona) {
        LocalDateTime inicioMes = hoje.withDayOfMonth(1).atStartOfDay(zona).toLocalDateTime();
        LocalDateTime fimMes = inicioMes.plusMonths(1).minusNanos(1);

        Set<UUID> clientesComHistoricoAnterior = atendimentoRepository
                .findByClinicaAndDataAtendimentoBefore(clinica, inicioMes).stream()
                .map(atendimento -> atendimento.getCliente().getId())
                .collect(Collectors.toSet());

        Set<UUID> clientesDoMes = atendimentoRepository
                .findByClinicaAndDataAtendimentoBetween(clinica, inicioMes, fimMes).stream()
                .map(atendimento -> atendimento.getCliente().getId())
                .collect(Collectors.toSet());

        int recorrentes = (int) clientesDoMes.stream()
                .filter(clientesComHistoricoAnterior::contains)
                .count();
        int novos = clientesDoMes.size() - recorrentes;
        return new int[] { novos, recorrentes };
    }
}