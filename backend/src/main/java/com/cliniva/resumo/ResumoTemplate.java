package com.cliniva.resumo;

public final class ResumoTemplate {

    private ResumoTemplate() {
    }

    public static String gerar(ResumoContexto contexto) {
        StringBuilder resumo = new StringBuilder();
        resumo.append("Hoje: ").append(contexto.atendimentosHoje())
                .append(atendimentoPalavra(contexto.atendimentosHoje()))
                .append(", receita prevista de ").append(moeda(contexto.receitaPrevista()))
                .append(" e receita realizada de ").append(moeda(contexto.receitaRealizada()))
                .append(".");
        resumo.append(" No mês: ").append(contexto.novosMes())
                .append(" cliente(s) nova(s) e ").append(contexto.recorrentesMes())
                .append(" que voltaram.");
        if (contexto.aniversariantesHoje() > 0) {
            resumo.append(" ").append(contexto.aniversariantesHoje())
                    .append(" aniversariante(s) hoje.");
        }
        if (!contexto.itensAbaixoMinimo().isEmpty()) {
            resumo.append(" Estoque baixo: ").append(String.join(", ", contexto.itensAbaixoMinimo()))
                    .append(".");
        }
        return resumo.toString();
    }

    private static String atendimentoPalavra(int quantidade) {
        return quantidade == 1 ? " atendimento" : " atendimentos";
    }

    private static String moeda(java.math.BigDecimal valor) {
        return "R$ " + valor.setScale(2).toPlainString().replace('.', ',');
    }
}