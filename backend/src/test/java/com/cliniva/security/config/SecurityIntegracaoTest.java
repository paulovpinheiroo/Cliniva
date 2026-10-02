package com.cliniva.security.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.cliniva.tenancy.Clinica;
import com.cliniva.tenancy.ClinicaRepository;
import com.cliniva.tenancy.Papel;
import com.cliniva.tenancy.Usuario;
import com.cliniva.tenancy.UsuarioRepository;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityIntegracaoTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ClinicaRepository clinicaRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

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
    void onboardingPublicoDeveCriarClinicaComSlugEExpediente() throws Exception {
        mockMvc.perform(post("/api/public/onboarding")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nomeClinica": "Clínica Convite Ação",
                                  "nomeResponsavel": "Dona Ana",
                                  "email": "dona.ana@email.com"
                                }"""))
                .andExpect(status().isCreated());

        Clinica criada = clinicaRepository.findByNome("Clínica Convite Ação").orElseThrow();
        // slug normalizado (sem acento) — antes a clínica ficava sem link público
        org.assertj.core.api.Assertions.assertThat(criada.getSlug()).isEqualTo("clinica-convite-acao");
    }

    @Test
    void onboardingNaoDeveRebaixarContaAdmin() throws Exception {
        // simula o ADMIN master do seed da migration 03: sem clínica, papel ADMIN
        Usuario admin = new Usuario();
        admin.setEmail("paulovictorpinheiro998663264@gmail.com");
        admin.setNome("Administrador Cliniva");
        admin.setPapel(Papel.ADMIN);
        admin.setAtivo(true);
        usuarioRepository.save(admin);

        mockMvc.perform(post("/api/public/onboarding")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nomeClinica": "Clínica Tentativa",
                                  "nomeResponsavel": "Tentativa",
                                  "email": "paulovictorpinheiro998663264@gmail.com"
                                }"""))
                .andExpect(status().isForbidden());
    }

    @Test
    void meSemTokenDeveRetornar401() throws Exception {
        mockMvc.perform(get("/api/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void agendaInternaDeveExigirAutenticacao() throws Exception {
        mockMvc.perform(get("/api/agenda?data=2026-09-28"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/agenda/link"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/agenda/horarios"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void bookingPublicoDeveResponder404ParaClinicaInexistente() throws Exception {
        mockMvc.perform(get("/api/public/booking/clinica-que-nao-existe/servicos"))
                .andExpect(status().isNotFound());
    }

    @Test
    void bookingPublicoDeveIgnorarClinicaInativa() throws Exception {
        Clinica inativa = new Clinica();
        inativa.setNome("Clínica Inativa");
        inativa.setSlug("clinica-inativa");
        inativa.setAtiva(false);
        clinicaRepository.save(inativa);

        mockMvc.perform(get("/api/public/booking/clinica-inativa/servicos"))
                .andExpect(status().isNotFound());
    }

    @Test
    void bookingPublicoDeveRejeitarPayloadInvalido() throws Exception {
        mockMvc.perform(post("/api/public/booking/clinica-inexistente")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "servicoId": "%s",
                                  "dataHora": "2026-10-05T10:00:00",
                                  "nome": "A",
                                  "telefone": "abc"
                                }""".formatted(UUID.randomUUID())))
                .andExpect(status().isBadRequest());
    }
}
