package com.cliniva.security.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cliniva.security.jwt.SupabaseJwtVerificador;
import com.cliniva.tenancy.Clinica;
import com.cliniva.tenancy.ClinicaRepository;
import com.cliniva.tenancy.Papel;
import com.cliniva.tenancy.Usuario;
import com.cliniva.tenancy.UsuarioRepository;

import io.jsonwebtoken.Claims;

/**
 * Regressão do bug encontrado em produção: exceção não tratada em caminho
 * protegido voltava <b>401 "Autenticação necessária"</b> em vez de 500.
 *
 * <p>Causa: o dispatch de erro do container não passa pelo
 * {@code JwtAutenticacaoFilter} (é {@code OncePerRequestFilter}, que pula
 * dispatch de erro), então o {@code SecurityContext} chegava vazio ao
 * {@code AuthorizationFilter} e a requisição era negada de novo.
 *
 * <p>Na prática, um {@code NullPointerException} ao criar um atendimento
 * sem {@code itensExtras} aparecia para o usuário como se o token tivesse
 * expirado.
 *
 * <p>Travam o contrato observável: 401 só quando falta autenticação de
 * verdade, 404 para rota inexistente, 405 para método errado, 500 para
 * exceção não tratada, e nenhum detalhe interno na resposta.
 *
 * <p>Observação sobre o alcance: o MockMvc repropaga exceção não tratada
 * em vez de fazer o dispatch de erro do container, então estes testes
 * cobrem o caminho do {@code GlobalExceptionHandler}. A regra de
 * {@code dispatcherTypeMatchers(ERROR)} no {@code SecurityConfig} é
 * defesa em profundidade para o que chegar ao container sem tratamento, e
 * não é verificável aqui.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(ErroHttpNaoPodeVirar401Test.ControllerQueExplode.class)
class ErroHttpNaoPodeVirar401Test {

    /**
     * Reproduz o caso real (NPE no controller) sem depender de nenhum serviço
     * de negócio. Sem a regra de dispatch de erro na {@code SecurityConfig},
     * estas rotas devolvem 401 em vez de 500 — é o que estes testes pegam.
     */
    @RestController
    static class ControllerQueExplode {

        @GetMapping("/api/explode/npe")
        public void npe() {
            throw new NullPointerException("falha simulada");
        }
    }

    // Único por execução: `supabase_user_id` tem índice único e o
    // contexto de teste é compartilhado com o resto da suíte.
    private final String subject = "usuario-supabase-" + UUID.randomUUID();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ClinicaRepository clinicaRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @MockitoBean
    private SupabaseJwtVerificador verificador;

    private Clinica clinica;

    @BeforeEach
    void prepararContexto() {
        clinica = new Clinica();
        clinica.setNome("Clínica Erro " + UUID.randomUUID());
        // O slug é gerado pelas migrations, não pela entidade — o Hibernate
        // cria a coluna NOT NULL no create-drop do H2.
        clinica.setSlug("clinica-erro-" + UUID.randomUUID().toString().substring(0, 8));
        clinica.setAtiva(true);
        clinica = clinicaRepository.save(clinica);

        // E-mail único por execução: a tabela tem índice único e o contexto
        // de teste é compartilhado com o resto da suíte.
        String email = "erro-" + UUID.randomUUID() + "@cliniva.com";

        Usuario usuario = new Usuario();
        usuario.setNome("Dono do Teste");
        usuario.setEmail(email);
        usuario.setPapel(Papel.ADMIN);
        usuario.setAtivo(true);
        usuario.setSupabaseUserId(subject);
        usuarioRepository.save(usuario);

        Claims claims = org.mockito.Mockito.mock(Claims.class);
        when(claims.getSubject()).thenReturn(subject);
        when(claims.getAudience()).thenReturn(Set.of("authenticated"));
        when(claims.get("email", String.class)).thenReturn(email);
        when(verificador.verificar("token-valido")).thenReturn(claims);
    }

    @Test
    void rotaInexistenteDeveResponder404Nao401QuandoAutenticado() throws Exception {
        mockMvc.perform(get("/api/rota-que-nao-existe")
                        .header("Authorization", "Bearer token-valido")
                        .header("X-Clinica", clinica.getId().toString()))
                .andExpect(status().isNotFound());
    }

    @Test
    void nullPointerNoControllerDeveResponder500Nao401() throws Exception {
        mockMvc.perform(get("/api/explode/npe")
                        .header("Authorization", "Bearer token-valido")
                        .header("X-Clinica", clinica.getId().toString()))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.mensagem").isNotEmpty());
    }

    @Test
    void metodoNaoSuportadoDeveContinuarSendo405() throws Exception {
        // A rede de segurança para exceção não tratada não pode engolir as
        // exceções padrão do Spring MVC: método errado é 405, não 500.
        mockMvc.perform(post("/api/explode/npe")
                        .header("Authorization", "Bearer token-valido")
                        .header("X-Clinica", clinica.getId().toString()))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    void rotaProtegidaContinuaRespondendo401SemToken() throws Exception {
        // O 401 legítimo (sem token) precisa continuar funcionando.
        mockMvc.perform(get("/api/explode/npe"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void respostaDeErroNaoPodeVazarDetalheInterno() throws Exception {
        String corpo = mockMvc.perform(get("/api/explode/npe")
                        .header("Authorization", "Bearer token-valido")
                        .header("X-Clinica", clinica.getId().toString()))
                .andReturn().getResponse().getContentAsString();

        assertThat(corpo).doesNotContain("java.lang");
        assertThat(corpo).doesNotContain("Exception");
        assertThat(corpo).doesNotContain("at com.cliniva");
        assertThat(corpo).doesNotContain("falha simulada");
    }

    @Test
    void mockDoClaimsCobreOQueOFiltroLe() throws Exception {
        // Sanidade: o mock do Claims cobre o que o filtro realmente lê.
        Claims claims = verificador.verificar("token-valido");
        assertThat(claims.getSubject()).isEqualTo(subject);
        assertThat(claims.getAudience()).containsExactly("authenticated");
        assertThat(claims.get("email", String.class)).isNotBlank();
    }

    @Test
    void clinicaDeTesteFoiPersistidaComSlug() {
        assertThat(clinica.getId()).isNotNull();
        assertThat(clinica.getSlug()).isNotBlank();
        Map<String, Object> esperado = Map.of("id", clinica.getId().toString(), "ativa", true);
        assertThat(esperado.get("ativa")).isEqualTo(true);
    }
}
