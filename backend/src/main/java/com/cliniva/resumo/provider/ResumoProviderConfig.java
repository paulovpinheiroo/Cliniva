package com.cliniva.resumo.provider;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Escolhe o provider do resumo do dia conforme {@code LLM_PROVIDER}.
 *
 * <p>Os defaults de modelo ficam <b>só</b> em {@code application.properties}.
 * Repetir o literal aqui duplicava a fonte da verdade, e foi exatamente assim
 * que o default do Groq ficou com um modelo inexistente sem ninguém perceber.
 */
@Configuration
public class ResumoProviderConfig {

    @Bean
    @ConditionalOnProperty(prefix = "cliniva.llm", name = "provider", havingValue = "gemini")
    public ResumoProvider geminiResumoProvider(
            @Value("${cliniva.llm.gemini.api-key}") String apiKey,
            @Value("${cliniva.llm.gemini.model}") String modelo) {
        return new GeminiResumoProvider(apiKey, modelo);
    }

    @Bean
    @ConditionalOnProperty(prefix = "cliniva.llm", name = "provider", havingValue = "groq")
    public ResumoProvider groqResumoProvider(
            @Value("${cliniva.llm.groq.api-key}") String apiKey,
            @Value("${cliniva.llm.groq.model}") String modelo) {
        return new GroqResumoProvider(apiKey, modelo);
    }
}