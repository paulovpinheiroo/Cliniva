package com.cliniva.config;

import java.time.Clock;
import java.time.ZoneId;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Relógio da aplicação, injetável para permitir testes determinísticos de
 * agenda. Zona padrão: America/Sao_Paulo (configurável via cliniva.zona).
 */
@Configuration
public class ClockConfig {

    @Bean
    public Clock clinivaClock(@Value("${cliniva.zona:America/Sao_Paulo}") String zona) {
        return Clock.system(ZoneId.of(zona));
    }

    /**
     * Para operações que NÃO podem ficar dentro de um {@code @Transactional}:
     * a geração do resumo do dia chama a API do provider de IA dentro do fluxo
     * e segurar transação durante a inferência prenderia uma conexão do pool
     * por vários segundos.
     */
    @Bean
    public TransactionTemplate clinivaTransactionTemplate(PlatformTransactionManager txManager) {
        return new TransactionTemplate(txManager);
    }
}
