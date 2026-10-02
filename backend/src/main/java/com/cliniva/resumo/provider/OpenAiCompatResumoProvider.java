package com.cliniva.resumo.provider;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import com.cliniva.resumo.ResumoContexto;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public abstract class OpenAiCompatResumoProvider implements ResumoProvider {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(3);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(5);

    private final RestClient client;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String modelo;

    protected OpenAiCompatResumoProvider(String baseUrl, String apiKey, String modelo) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout((int) CONNECT_TIMEOUT.toMillis());
        requestFactory.setReadTimeout((int) READ_TIMEOUT.toMillis());
        this.client = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .requestFactory(requestFactory)
                .build();
        this.modelo = modelo;
    }

    @Override
    public String gerar(ResumoContexto contexto) {
        Map<String, Object> corpo = Map.of(
                "model", modelo,
                "messages", List.of(Map.of("role", "user", "content", prompt(contexto))),
                "temperature", 0.4,
                "max_tokens", 256);
        String resposta = client.post()
                .uri("/chat/completions")
                .body(corpo)
                .retrieve()
                .body(String.class);
        return extrairTexto(resposta);
    }

    private String extrairTexto(String resposta) {
        try {
            JsonNode raiz = objectMapper.readTree(resposta);
            return raiz.path("choices").path(0).path("message").path("content").asText("");
        } catch (Exception excecao) {
            throw new IllegalStateException("Resposta do provedor de IA inválida", excecao);
        }
    }

    protected abstract String prompt(ResumoContexto contexto);
}