package com.cliniva.resumo.provider;

import com.cliniva.resumo.ResumoContexto;

public class GeminiResumoProvider extends OpenAiCompatResumoProvider {

    public GeminiResumoProvider(String apiKey, String modelo) {
        super("https://generativelanguage.googleapis.com/v1beta/openai", apiKey, modelo);
    }

    @Override
    protected String prompt(ResumoContexto contexto) {
        return ResumoPrompt.compor(contexto);
    }
}