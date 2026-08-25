package com.cliniva.atendimento.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import com.cliniva.atendimento.enums.StatusAtendimento;
import com.cliniva.exception.TransicaoStatusInvalidaException;

class AtendimentoTest {

    private Atendimento atendimentoComStatus(StatusAtendimento status) {
        Atendimento atendimento = new Atendimento();
        atendimento.setStatus(status);
        return atendimento;
    }

    @Test
    void novoAtendimentoAceitaAgendado() {
        assertThatCode(() -> new Atendimento().setStatus(StatusAtendimento.AGENDADO))
                .doesNotThrowAnyException();
    }

    @Test
    void agendadoPodeVirarConcluido() {
        Atendimento atendimento = atendimentoComStatus(StatusAtendimento.AGENDADO);

        atendimento.setStatus(StatusAtendimento.CONCLUIDO);

        assertThat(atendimento.getStatus()).isEqualTo(StatusAtendimento.CONCLUIDO);
    }

    @Test
    void canceladoNaoPodeMaisSerAlterado() {
        Atendimento cancelado = atendimentoComStatus(StatusAtendimento.CANCELADO);

        assertThatThrownBy(() -> cancelado.setStatus(StatusAtendimento.AGENDADO))
                .isInstanceOf(TransicaoStatusInvalidaException.class);

        assertThatThrownBy(() -> cancelado.setStatus(StatusAtendimento.CANCELADO))
                .isInstanceOf(TransicaoStatusInvalidaException.class);
    }
}
