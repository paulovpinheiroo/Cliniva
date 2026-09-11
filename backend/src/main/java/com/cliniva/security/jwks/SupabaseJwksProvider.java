package com.cliniva.security.jwks;

import java.math.BigInteger;
import java.security.AlgorithmParameters;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.ECPublicKeySpec;
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
    public Map<String, PublicKey> obterChaves() {
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

        Map<String, PublicKey> chaves = new HashMap<>();
        for (Jwk chave : resposta.keys()) {
            chaves.put(chave.kid(), construirChave(chave));
        }
        return chaves;
    }

    private PublicKey construirChave(Jwk chave) {
        try {
            if ("RSA".equals(chave.kty())) {
                byte[] exponente = Base64.getUrlDecoder().decode(chave.e());
                byte[] modulo = Base64.getUrlDecoder().decode(chave.n());
                RSAPublicKeySpec spec = new RSAPublicKeySpec(new BigInteger(1, modulo),
                        new BigInteger(1, exponente));
                return KeyFactory.getInstance("RSA").generatePublic(spec);
            }
            if ("EC".equals(chave.kty())) {
                return chaveEc(chave);
            }
            throw new InvalidKeySpecException("Tipo de chave não suportado: " + chave.kty());
        } catch (IllegalArgumentException | NoSuchAlgorithmException | InvalidKeySpecException
                | java.security.spec.InvalidParameterSpecException e) {
            throw new JwtInvalidoException("Chave JWKS inválida: " + chave.kid());
        }
    }

    private PublicKey chaveEc(Jwk chave)
            throws NoSuchAlgorithmException, InvalidKeySpecException, java.security.spec.InvalidParameterSpecException {
        String curva = switch (chave.crv()) {
            case "P-256" -> "secp256r1";
            case "P-384" -> "secp384r1";
            case "P-521" -> "secp521r1";
            default -> throw new InvalidKeySpecException("Curva EC não suportada: " + chave.crv());
        };
        byte[] x = Base64.getUrlDecoder().decode(chave.x());
        byte[] y = Base64.getUrlDecoder().decode(chave.y());
        AlgorithmParameters parametros = AlgorithmParameters.getInstance("EC");
        parametros.init(new ECGenParameterSpec(curva));
        ECParameterSpec spec = parametros.getParameterSpec(ECParameterSpec.class);
        ECPublicKeySpec chavePublica = new ECPublicKeySpec(
                new ECPoint(new BigInteger(1, x), new BigInteger(1, y)), spec);
        return KeyFactory.getInstance("EC").generatePublic(chavePublica);
    }

    public record Jwk(String kty, String kid, String use, String alg, String n, String e, String crv,
            String x, String y) {
    }

    public record RespostaJwks(List<Jwk> keys) {
    }
}