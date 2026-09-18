package com.cliniva.resumo;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import com.cliniva.resumo.model.ResumoDiaCache.Origem;

public record ResumoDoDiaDTO(
        int atendimentosHoje,
        BigDecimal receitaPrevista,
        BigDecimal receitaRealizada,
        int novosMes,
        int recorrentesMes,
        int aniversariantesHoje,
        List<String> itensAbaixoMinimo,
        String texto,
        Origem origem,
        Instant geradoEm,
        int tentativasRestantes) {
}