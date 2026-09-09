package com.cliniva.security.jwks;

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.cliniva.security.jwt.JwtInvalidoException;

@Component
public class SupabaseJwksProvider implements JwksProvider {

    private final RestClient restClient;
    private final String supabaseUrl;

    public SupabaseJwksProvider(RestClient restClient,
            @Value("${cliniva.supabase.url:}") String supabaseUrl) {
        this.restClient = restClient;
        this.supabaseUrl = supabaseUrl;
    }

    @Override
    public Map<String, RSAPublicKey> obterChaves() {
        if (supabaseUrl == null || supabaseUrl.isBlank()) {
            throw new JwtInvalidoException("SUPABASE_URL não configurada");
        }
        RespostaJwks resposta = restClient.get()
                .uri(supabaseUrl + "/auth/v1/.well-known/jwks.json")
                .retrieve()
                .body(RespostaJwks.class);

        if (resposta == null || resposta.keys() == null) {
            throw new JwtInvalidoException("JWKS do Supabase retornou resposta vazia");
        }

        Map<String, RSAPublicKey> chaves = new HashMap<>();
        for (Jwk chave : resposta.keys()) {
            try {
                byte[] exponente = Base64.getUrlDecoder().decode(chave.e());
                byte[] modulo = Base64.getUrlDecoder().decode(chave.n());
                RSAPublicKeySpec spec = new RSAPublicKeySpec(new BigInteger(1, modulo),
                        new BigInteger(1, exponente));
                chaves.put(chave.kid(), (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(spec));
            } catch (IllegalArgumentException | NoSuchAlgorithmException | InvalidKeySpecException e) {
                throw new JwtInvalidoException("Chave JWKS inválida: " + chave.kid());
            }
        }
        return chaves;
    }

    public record Jwk(String kty, String kid, String use, String alg, String n, String e) {
    }

    public record RespostaJwks(List<Jwk> keys) {
    }
}