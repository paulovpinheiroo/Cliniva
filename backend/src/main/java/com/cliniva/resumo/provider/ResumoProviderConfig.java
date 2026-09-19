package com.cliniva.resumo.provider;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ResumoProviderConfig {

    @Bean
    @ConditionalOnProperty(prefix = "cliniva.llm", name = "provider", havingValue = "gemini")
    public ResumoProvider geminiResumoProvider(
            @Value("${cliniva.llm.gemini.api-key}") String apiKey,
            @Value("${cliniva.llm.gemini.model:gemini-3.6-flash}") String modelo) {
        return new GeminiResumoProvider(apiKey, modelo);
    }

    @Bean
    @ConditionalOnProperty(prefix = "cliniva.llm", name = "provider", havingValue = "groq")
    public ResumoProvider groqResumoProvider(
            @Value("${cliniva.llm.groq.api-key}") String apiKey,
            @Value("${cliniva.llm.groq.model:llama-3.3-70b-versatile}") String modelo) {
        return new GroqResumoProvider(apiKey, modelo);
    }
}