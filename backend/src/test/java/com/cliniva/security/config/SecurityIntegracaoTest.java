package com.cliniva.security.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityIntegracaoTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void rotaProtegidaDeveRetornar401SemToken() throws Exception {
        mockMvc.perform(get("/api/clientes"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rotaAdminDeveNegarAcessoParaNaoAutenticado() throws Exception {
        mockMvc.perform(get("/api/admin/clinicas"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void onboardingPublicoDeveCriarClinica() throws Exception {
        mockMvc.perform(post("/api/public/onboarding")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nomeClinica": "Clínica Integração",
                                  "nomeResponsavel": "Dona Maria",
                                  "email": "dona.maria@email.com"
                                }"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.clinicaNome").value("Clínica Integração"));
    }

    @Test
    void meSemTokenDeveRetornar401() throws Exception {
        mockMvc.perform(get("/api/me"))
                .andExpect(status().isUnauthorized());
    }
}