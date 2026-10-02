package com.cliniva.booking;

/** Excedeu o limite de agendamentos públicos por origem dentro da janela. */
public class RateLimitExcedidoException extends RuntimeException {

    public RateLimitExcedidoException() {
        super("Muitas tentativas de agendamento. Aguarde alguns minutos e tente novamente.");
    }
}
