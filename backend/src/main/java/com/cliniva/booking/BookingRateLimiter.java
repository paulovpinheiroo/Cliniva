package com.cliniva.booking;

import java.time.Clock;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

/**
 * Rate limit simples, em memória, para o booking público (rotas abertas).
 * <p>
 * Objetivo: impedir que um script automatizado preencha a agenda da clínica.
 * Janela deslizante aproximada por contagem incremental; suficiente para o
 * free tier e sem adicionar dependência externa.
 * </p>
 * <p>
 * A chave é <b>slug + IP</b> de propósito. Incluir o telefone na chave
 * tornaria o limite inútil: bastaria trocar o número a cada requisição para
 * zerar o contador — exatamente o ataque que a classe existe para impedir.
 * </p>
 */
@Component
@RequiredArgsConstructor
public class BookingRateLimiter {

    /** Tentativas de agendamento por origem dentro da janela. */
    private static final int MAX_TENTATIVAS = 5;

    private static final Duration JANELA = Duration.ofMinutes(10);

    /** A cada N registros, poda as janelas já expiradas. */
    private static final int PODAR_A_CADA = 200;

    private final Clock clock;
    private final Map<String, Contador> acessos = new ConcurrentHashMap<>();
    private final AtomicLong registros = new AtomicLong();

    public void registrar(String chave) {
        long agora = clock.millis();
        Contador contador = acessos.compute(chave, (k, atual) -> {
            if (atual == null || agora - atual.inicioJanela > JANELA.toMillis()) {
                return new Contador(agora, 1);
            }
            return new Contador(atual.inicioJanela, atual.tentativas.incrementAndGet());
        });

        if (registros.incrementAndGet() % PODAR_A_CADA == 0) {
            podar(agora);
        }

        if (contador.tentativas.get() > MAX_TENTATIVAS) {
            throw new RateLimitExcedidoException();
        }
    }

    /**
     * Remove janelas expiradas. Sem isso o mapa cresce sem limite, já que a
     * chave inclui o IP e cada origem nova adiciona uma entrada que nunca
     * mais sai sozinha.
     */
    private void podar(long agora) {
        acessos.entrySet().removeIf(entrada -> agora - entrada.getValue().inicioJanela > JANELA.toMillis());
    }

    private record Contador(long inicioJanela, AtomicInteger tentativas) {
        Contador(long inicioJanela, int tentativas) {
            this(inicioJanela, new AtomicInteger(tentativas));
        }
    }

    /** Só para teste: quantas origens estão guardadas agora. */
    int tamanho() {
        return acessos.size();
    }
}
