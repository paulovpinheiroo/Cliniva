package com.cliniva.resumo.provider;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.junit.jupiter.api.Test;

/**
 * Trava a configuração do provider de IA.
 *
 * <p>Existe por causa de um bug real: o default da Groq era
 * {@code groq/compound-mini}, um modelo que <b>não existe</b> na API da
 * Groq. Com ele, toda chamada falhava e o resumo caía no template — sem
 * erro na tela, porque o `ResumoService` engole a exceção. Duas pessoas
 * leram a issue que dizia "os defaults já estão corrigidos" e acreditaram.
 *
 * <p>Este teste não consegue chamar a API do provider (não há chave nos
 * testes, e não deveria haver). O que ele faz é obrigar uma revisão
 * consciente: trocar a lista abaixo exige olhar a documentação oficial do
 * provider. Os links ficam no comentário.
 *
 * <p>E nem a documentação basta: nem todo modelo listado está acessível
 * para toda conta. Com a chave deste projeto, `llama-3.1-8b-instant` e
 * `llama-3.3-70b-versatile` — ambos na documentação — retornam
 * `model_not_found`. Por isso a lista abaixo é o que a API respondeu em
 * 2026-09-30, não o que a doc prometia. Para revalidar:
 *
 * <pre>
 * curl -s https://api.groq.com/openai/v1/models -H "Authorization: Bearer $CHAVE"
 * </pre>
 *
 *
 * <ul>
 * <li>Gemini: https://ai.google.dev/gemini-api/docs/models</li>
 * <li>Groq: https://console.groq.com/docs/models</li>
 * </ul>
 *
 * <p>Última conferência manual: 2026-09-30.
 */
class LlmConfigTest {

    /**
     * Modelos aceitos. Ao adicionar ou trocar um, confira a lista oficial do
     * provider primeiro e atualize a data abaixo.
     */
    private static final Set<String> MODELOS_GEMINI = Set.of(
            "gemini-3.5-flash-lite",
            "gemini-3.8-flash",
            "gemini-3.6-flash",
            "gemini-2.5-flash");

    private static final Set<String> MODELOS_GROQ = Set.of(
            "openai/gpt-oss-20b",
            "openai/gpt-oss-120b",
            "qwen/qwen3.8-27b",
            "allam-2-7b");

    /**
     * Lê o {@code application.properties} <b>do main</b> pelo filesystem, e
     * não pelo classpath: {@code src/test/resources/application.properties}
     * sobrescreve o arquivo no classpath e não tem as chaves de LLM.
     */
    private static Properties applicationProperties() throws IOException {
        Properties props = new Properties();
        try (InputStream in = java.nio.file.Files.newInputStream(java.nio.file.Path.of(
                "src/main/resources/application.properties"))) {
            props.load(in);
        }
        return props;
    }

    /** Extrai o valor default de uma expressão tipo `${VAR:default}`. */
    private static String defaultDe(String expressao) {
        int inicio = expressao.indexOf(':');
        assertThat(inicio).as("esperado ${VAR:default}, veio %s", expressao).isPositive();
        return expressao.substring(inicio + 1, expressao.length() - 1);
    }

    @Test
    void defaultDoGeminiDeveSerUmModeloConhecido() throws IOException {
        String expressao = applicationProperties().getProperty("cliniva.llm.gemini.model");
        String modelo = defaultDe(expressao);

        assertThat(MODELOS_GEMINI)
                .as("modelo '%s' fora da lista conhecida da Gemini — confira "
                        + "https://ai.google.dev/gemini-api/docs/models", modelo)
                .contains(modelo);
    }

    @Test
    void defaultDoGroqDeveSerUmModeloConhecido() throws IOException {
        String expressao = applicationProperties().getProperty("cliniva.llm.groq.model");
        String modelo = defaultDe(expressao);

        assertThat(MODELOS_GROQ)
                .as("modelo '%s' fora da lista conhecida da Groq — confira "
                        + "https://console.groq.com/docs/models", modelo)
                .contains(modelo);
    }

    @Test
    void providerVazioPorPadraoParaNaoFazerChamadaExterna() throws IOException {
        String provider = applicationProperties().getProperty("cliniva.llm.provider");

        assertThat(defaultDe(provider))
                .as("LLM_PROVIDER vazio por padrão: sem isso, o app local chama a API sem querer")
                .isEmpty();
    }

    @Test
    void configNaoPodeRepetirODefaultDoModelo() throws Exception {
        // Os defaults ficam só em application.properties. Repetir o literal no
        // @Value foi o que deixou o valor errado passar despercebido.
        String fonte = new String(java.nio.file.Files.readAllBytes(java.nio.file.Path.of(
                "src/main/java/com/cliniva/resumo/provider/ResumoProviderConfig.java")));

        assertThat(fonte)
                .as("ResumoProviderConfig não deve ter default de modelo embutido — "
                        + "a fonte da verdade é application.properties")
                .doesNotContain("${cliniva.llm.gemini.model:");
        assertThat(fonte)
                .doesNotContain("${cliniva.llm.groq.model:");
    }
}
