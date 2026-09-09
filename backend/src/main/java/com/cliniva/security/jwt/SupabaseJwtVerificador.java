package com.cliniva.security.jwt;

import java.nio.charset.StandardCharsets;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.cliniva.security.jwks.JwksProvider;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;

/**
 * Valida tokens de acesso do Supabase Auth (RS256, assinatura verificada via
 * JWKS publicada em {@code /auth/v1/.well-known/jwks.json}).
 */
@Component
public class SupabaseJwtVerificador {

    private final JwksProvider jwksProvider;
    private final String supabaseUrl;
    private final Map<String, RSAPublicKey> chaves = new ConcurrentHashMap<>();
    private volatile Instant chavesCarregadasEm;

    public SupabaseJwtVerificador(JwksProvider jwksProvider,
            @Value("${cliniva.supabase.url:}") String supabaseUrl) {
        this.jwksProvider = jwksProvider;
        this.supabaseUrl = supabaseUrl;
    }

    public Claims verificar(String token) throws JwtException {
        if (token == null || token.isBlank()) {
            throw new JwtInvalidoException("Token ausente");
        }
        String kid = extrairKid(token);
        RSAPublicKey chave = chaveDoKid(kid);
        Claims claims = Jwts.parser().verifyWith(chave).build()
                .parseSignedClaims(token).getPayload();

        if (claims.getAudience() == null || !claims.getAudience().contains("authenticated")) {
            throw new JwtInvalidoException("Token não é um token de usuário autenticado");
        }
        if (supabaseUrl != null && !supabaseUrl.isBlank()
                && claims.getIssuer() != null
                && !claims.getIssuer().equals(supabaseUrl + "/auth/v1")) {
            throw new JwtInvalidoException("Emitente do token não corresponde ao Supabase configurado");
        }
        return claims;
    }

    private RSAPublicKey chaveDoKid(String kid) {
        RSAPublicKey chave = chaves.get(kid);
        if (chave == null) {
            recarregarChaves();
            chave = chaves.get(kid);
            if (chave == null) {
                throw new JwtInvalidoException("Nenhuma chave JWKS conhecida para kid " + kid);
            }
        }
        return chave;
    }

    private void recarregarChaves() {
        Map<String, RSAPublicKey> novas = jwksProvider.obterChaves();
        chaves.clear();
        chaves.putAll(novas);
    }

    private String extrairKid(String token) {
        try {
            String[] partes = token.split("\\.");
            if (partes.length != 3) {
                throw new JwtInvalidoException("Token JWT malformado");
            }
            String header = new String(Base64.getUrlDecoder().decode(partes[0]), StandardCharsets.UTF_8);
            int inicio = header.indexOf("\"kid\"");
            if (inicio < 0) {
                throw new JwtInvalidoException("Token JWT sem kid no cabeçalho");
            }
            int doisPontos = header.indexOf(':', inicio);
            int aspasInicio = header.indexOf('"', doisPontos);
            int aspasFim = header.indexOf('"', aspasInicio + 1);
            return header.substring(aspasInicio + 1, aspasFim);
        } catch (IllegalArgumentException e) {
            throw new JwtInvalidoException("Token JWT malformado");
        }
    }
}