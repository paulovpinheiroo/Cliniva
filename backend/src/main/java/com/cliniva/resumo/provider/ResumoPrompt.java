package com.cliniva.resumo.provider;

import java.math.BigDecimal;

import com.cliniva.resumo.ResumoContexto;

public final class ResumoPrompt {

    private ResumoPrompt() {
    }

    public static String compor(ResumoContexto contexto) {
        return "Você é o assistente de gestão de uma clínica de estética chamada "
                + contexto.nomeClinica()
                + ". Escreva um resumo matinal do dia para a dona do estabelecimento, em português "
                + "do Brasil, com no máximo 3 frases curtas e diretas, sem listas nem emojis, "
                + "com tom de briefing de começo de dia.\n\n"
                + "Dados do dia (apenas números agregados):\n"
                + "- Atendimentos hoje (exceto cancelados): " + contexto.atendimentosHoje() + "\n"
                + "- Receita prevista hoje (agendados): " + moeda(contexto.receitaPrevista()) + "\n"
                + "- Receita realizada hoje (concluídos): " + moeda(contexto.receitaRealizada()) + "\n"
                + "- Novas clientes no mês: " + contexto.novosMes() + "\n"
                + "- Clientes que voltaram no mês: " + contexto.recorrentesMes() + "\n"
                + "- Aniversariantes hoje: " + contexto.aniversariantesHoje() + "\n"
                + "- Produtos com estoque baixo (menos de 5): "
                + (contexto.itensAbaixoMinimo().isEmpty() ? "nenhum"
                        : String.join(", ", contexto.itensAbaixoMinimo()))
                + "\n\n"
                + "Responda apenas com o resumo, sem se dirigir à dona pelo nome, sem perguntar nada "
                + "e sem citar nomes ou dados de clientes individuais.";
    }

    private static String moeda(BigDecimal valor) {
        return "R$ " + valor.setScale(2).toPlainString().replace('.', ',');
    }
}