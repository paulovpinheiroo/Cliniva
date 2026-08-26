package com.cliniva.atendimento;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.domain.Specification;

import com.cliniva.atendimento.enums.StatusAtendimento;
import com.cliniva.atendimento.model.Atendimento;

public class AtendimentoSpecifications {

    public static Specification<Atendimento> comFiltros(StatusAtendimento status, UUID clienteId,
            LocalDateTime dataInicio, LocalDateTime dataFim) {
        List<Specification<Atendimento>> especificacoes = new ArrayList<>();

        if (status != null) {
            especificacoes.add(comStatus(status));
        }
        if (clienteId != null) {
            especificacoes.add(comCliente(clienteId));
        }
        if (dataInicio != null && dataFim != null) {
            especificacoes.add(comPeriodo(dataInicio, dataFim));
        }

        return Specification.allOf(especificacoes);
    }

    private static Specification<Atendimento> comStatus(StatusAtendimento status) {
        return (root, query, builder) -> builder.equal(root.get("status"), status);
    }

    private static Specification<Atendimento> comCliente(UUID clienteId) {
        return (root, query, builder) -> builder.equal(root.get("cliente").get("id"), clienteId);
    }

    private static Specification<Atendimento> comPeriodo(LocalDateTime inicio, LocalDateTime fim) {
        return (root, query, builder) -> builder.and(
                builder.greaterThanOrEqualTo(root.get("dataAtendimento"), inicio),
                builder.lessThanOrEqualTo(root.get("dataAtendimento"), fim));
    }
}
