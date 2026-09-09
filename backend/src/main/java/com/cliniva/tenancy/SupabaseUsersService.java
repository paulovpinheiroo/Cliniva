package com.cliniva.tenancy;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.cliniva.exception.SupabaseIndisponivelException;

/**
 * Chama a API administrativa do Supabase Auth (requer a service role key).
 * Token neutro: usa o header {@code apikey} + Bearer da service role.
 */
@Service
public class SupabaseUsersService {

    private final RestClient restClient;
    private final String supabaseUrl;
    private final String serviceRoleKey;

    public SupabaseUsersService(RestClient restClient,
            @Value("${cliniva.supabase.url:}") String supabaseUrl,
            @Value("${cliniva.supabase.service-role-key:}") String serviceRoleKey) {
        this.supabaseUrl = supabaseUrl;
        this.serviceRoleKey = serviceRoleKey;
        if (configurada()) {
            this.restClient = RestClient.builder()
                    .baseUrl(trimTrailingBarra(supabaseUrl))
                    .defaultHeader("apikey", serviceRoleKey)
                    .defaultHeader("Authorization", "Bearer " + serviceRoleKey)
                    .build();
        } else {
            this.restClient = restClient;
        }
    }

    public boolean configurada() {
        return supabaseUrl != null && !supabaseUrl.isBlank()
                && serviceRoleKey != null && !serviceRoleKey.isBlank();
    }

    public UsuarioSupabase criarUsuario(String email, String senha) {
        exigirConfiguracao();
        UsuarioSupabase usuario = restClient.post()
                .uri("/auth/v1/admin/users")
                .contentType(MediaType.APPLICATION_JSON)
                .body(java.util.Map.of("email", email, "password", senha, "email_confirm", true))
                .retrieve()
                .body(UsuarioSupabase.class);
        if (usuario == null || usuario.id() == null) {
            throw new SupabaseIndisponivelException("Supabase não retornou o usuário criado");
        }
        return usuario;
    }

    public void definirSenha(String supabaseUserId, String novaSenha) {
        exigirConfiguracao();
        restClient.put()
                .uri("/auth/v1/admin/users/{id}", supabaseUserId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(java.util.Map.of("password", novaSenha))
                .retrieve()
                .toBodilessEntity();
    }

    public Optional<UsuarioSupabase> buscarPorEmail(String email) {
        exigirConfiguracao();
        UsuarioSupabase[] usuarios = restClient.get()
                .uri(uriBuilder -> uriBuilder.path("/auth/v1/admin/users")
                        .queryParam("email", email).build())
                .retrieve()
                .body(UsuarioSupabase[].class);
        if (usuarios == null) {
            return Optional.empty();
        }
        return Arrays.stream(usuarios)
                .filter(u -> u.email() != null && u.email().equalsIgnoreCase(email))
                .findFirst();
    }

    private void exigirConfiguracao() {
        if (!configurada()) {
            throw new SupabaseIndisponivelException(
                    "Integração com Supabase não configurada (SUPABASE_URL / SUPABASE_SERVICE_ROLE_KEY)");
        }
    }

    private String trimTrailingBarra(String valor) {
        return valor.endsWith("/") ? valor.substring(0, valor.length() - 1) : valor;
    }

    public record UsuarioSupabase(String id, String email) {
    }
}