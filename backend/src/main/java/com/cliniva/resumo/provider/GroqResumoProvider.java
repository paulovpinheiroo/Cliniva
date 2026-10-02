package com.cliniva.resumo.provider;

import com.cliniva.resumo.ResumoContexto;

public class GroqResumoProvider extends OpenAiCompatResumoProvider {

    public GroqResumoProvider(String apiKey, String modelo) {
        super("https://api.groq.com/openai/v1", apiKey, modelo);
    }

    @Override
    protected String prompt(ResumoContexto contexto) {
        return ResumoPrompt.compor(contexto);
    }
}