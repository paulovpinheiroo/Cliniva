package com.cliniva.tenancy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestToUriTemplate;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.cliniva.tenancy.SupabaseUsersService.UsuarioSupabase;

class SupabaseUsersServiceTest {

    private RestClient.Builder builder;
    private MockRestServiceServer server;
    private SupabaseUsersService servico;

    @BeforeEach
    void setUp() {
        builder = RestClient.builder().baseUrl("https://x.supabase.co");
        server = MockRestServiceServer.bindTo(builder).build();
        servico = new SupabaseUsersService(builder.build(), "https://x.supabase.co", "segredo");
        ReflectionTestUtils.setField(servico, "restClient", builder.build());
    }

    @Test
    void buscarPorEmail_mapeiaPayloadComWrapperUsers() {
        String payload = """
                {"users":[{"id":"59fed22d-eade-4463-9a06-2b80d5bdb336",\
                "email":"admin@cliniva.com",\
                "app_metadata":{"provider":"email"},"identities":null}]}
                """;
        server.expect(requestToUriTemplate("https://x.supabase.co/auth/v1/admin/users?email=admin@cliniva.com"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(payload, MediaType.APPLICATION_JSON));

        Optional<UsuarioSupabase> resultado = servico.buscarPorEmail("admin@cliniva.com");

        assertTrue(resultado.isPresent());
        assertEquals("59fed22d-eade-4463-9a06-2b80d5bdb336", resultado.get().id());
        assertEquals("admin@cliniva.com", resultado.get().email());
    }

    @Test
    void buscarPorEmail_ignoraCaseNoEmail() {
        String payload = """
                {"users":[{"id":"abc","email":"Admin@Cliniva.COM",\
                "app_metadata":{},"identities":null}]}
                """;
        server.expect(requestToUriTemplate("https://x.supabase.co/auth/v1/admin/users?email=admin@cliniva.com"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(payload, MediaType.APPLICATION_JSON));

        Optional<UsuarioSupabase> resultado = servico.buscarPorEmail("admin@cliniva.com");

        assertTrue(resultado.isPresent());
        assertEquals("abc", resultado.get().id());
    }

    @Test
    void buscarPorEmail_retornaVazioQuandoListaVazia() {
        server.expect(requestToUriTemplate("https://x.supabase.co/auth/v1/admin/users?email=nada@cliniva.com"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"users\":[]}", MediaType.APPLICATION_JSON));

        assertFalse(servico.buscarPorEmail("nada@cliniva.com").isPresent());
    }
}