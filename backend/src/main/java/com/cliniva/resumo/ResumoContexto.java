package com.cliniva.resumo;

import java.math.BigDecimal;
import java.util.List;

public record ResumoContexto(
        String nomeClinica,
        int atendimentosHoje,
        BigDecimal receitaPrevista,
        BigDecimal receitaRealizada,
        int novosMes,
        int recorrentesMes,
        int aniversariantesHoje,
        List<String> itensAbaixoMinimo) {
}