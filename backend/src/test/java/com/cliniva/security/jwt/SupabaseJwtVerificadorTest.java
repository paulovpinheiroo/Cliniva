package com.cliniva.security.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.spec.ECGenParameterSpec;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cliniva.security.jwks.JwksProvider;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;

@ExtendWith(MockitoExtension.class)
class SupabaseJwtVerificadorTest {

    private static final String SUPABASE_URL = "https://projeto.supabase.co";
    private static final String SUBJECT = UUID.randomUUID().toString();
    private static final String KID = "chave-1";
    private static final String KID_EC = "chave-ec-1";

    @Mock
    private JwksProvider jwksProvider;

    private SupabaseJwtVerificador verificador;
    private KeyPair chave;
    private KeyPair chaveEc;

    @BeforeEach
    void setUp() throws Exception {
        verificador = new SupabaseJwtVerificador(jwksProvider, SUPABASE_URL);
        KeyPairGenerator gerador = KeyPairGenerator.getInstance("RSA");
        gerador.initialize(2048);
        chave = gerador.generateKeyPair();
        KeyPairGenerator geradorEc = KeyPairGenerator.getInstance("EC");
        geradorEc.initialize(new ECGenParameterSpec("secp256r1"));
        chaveEc = geradorEc.generateKeyPair();
    }

    private String tokenValido() {
        return Jwts.builder()
                .header().keyId(KID).and()
                .subject(SUBJECT)
                .claim("aud", "authenticated")
                .issuer(SUPABASE_URL + "/auth/v1")
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(Instant.now().plusSeconds(3600)))
                .signWith(chave.getPrivate(), Jwts.SIG.RS256)
                .compact();
    }

    @Test
    void deveAceitarTokenValidoComAssinaturaDoJwks() {
        when(jwksProvider.obterChaves())
                .thenReturn(Map.of(KID, chave.getPublic()));

        var claims = verificador.verificar(tokenValido());

        assertThat(claims.getSubject()).isEqualTo(SUBJECT);
    }

    @Test
    void deveAceitarTokenEs256AssinadoPorChaveEc() {
        when(jwksProvider.obterChaves())
                .thenReturn(Map.of(KID_EC, chaveEc.getPublic()));

        String tokenEs256 = Jwts.builder()
                .header().keyId(KID_EC).and()
                .subject(SUBJECT)
                .claim("aud", "authenticated")
                .issuer(SUPABASE_URL + "/auth/v1")
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(Instant.now().plusSeconds(3600)))
                .signWith(chaveEc.getPrivate(), Jwts.SIG.ES256)
                .compact();

        var claims = verificador.verificar(tokenEs256);

        assertThat(claims.getSubject()).isEqualTo(SUBJECT);
    }

    @Test
    void deveRejeitarTokenComAssinaturaModificada() {
        when(jwksProvider.obterChaves())
                .thenReturn(Map.of(KID, chave.getPublic()));

        String token = tokenValido() + "tamper";

        assertThatThrownBy(() -> verificador.verificar(token))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void deveRejeitarTokenExpirado() {
        when(jwksProvider.obterChaves())
                .thenReturn(Map.of(KID, chave.getPublic()));

        String tokenExpirado = Jwts.builder()
                .header().keyId(KID).and()
                .subject(SUBJECT)
                .claim("aud", "authenticated")
                .issuer(SUPABASE_URL + "/auth/v1")
                .issuedAt(Date.from(Instant.now().minusSeconds(7200)))
                .expiration(Date.from(Instant.now().minusSeconds(3600)))
                .signWith(chave.getPrivate(), Jwts.SIG.RS256)
                .compact();

        assertThatThrownBy(() -> verificador.verificar(tokenExpirado))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void deveRejeitarTokenDeServico() {
        when(jwksProvider.obterChaves())
                .thenReturn(Map.of(KID, chave.getPublic()));

        String tokenServico = Jwts.builder()
                .header().keyId(KID).and()
                .subject(SUBJECT)
                .claim("aud", "service_role")
                .issuer(SUPABASE_URL + "/auth/v1")
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(Instant.now().plusSeconds(3600)))
                .signWith(chave.getPrivate(), Jwts.SIG.RS256)
                .compact();

        assertThatThrownBy(() -> verificador.verificar(tokenServico))
                .isInstanceOf(JwtInvalidoException.class);
    }

    @Test
    void deveRecarregarJwksQuandoKidNaoConhecido() {
        when(jwksProvider.obterChaves())
                .thenReturn(Map.of())
                .thenReturn(Map.of(KID, chave.getPublic()));

        assertThatThrownBy(() -> verificador.verificar(tokenValido()))
                .isInstanceOf(JwtInvalidoException.class)
                .hasMessageContaining(KID);

        assertThat(verificador.verificar(tokenValido()).getSubject()).isEqualTo(SUBJECT);
    }

    @Test
    void naoDeveAceitarTokenDeEmitenteDiferente() {
        when(jwksProvider.obterChaves())
                .thenReturn(Map.of(KID, chave.getPublic()));

        String tokenOutroIssuer = Jwts.builder()
                .header().keyId(KID).and()
                .subject(SUBJECT)
                .claim("aud", "authenticated")
                .issuer("https://outro.supabase.co/auth/v1")
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(Instant.now().plusSeconds(3600)))
                .signWith(chave.getPrivate(), Jwts.SIG.RS256)
                .compact();

        assertThatThrownBy(() -> verificador.verificar(tokenOutroIssuer))
                .isInstanceOf(JwtInvalidoException.class)
                .hasMessageContaining("Emitente");
    }

    @Test
    void deveRejeitarChaveDesconhecidaMesmoAposRecarregar() {
        when(jwksProvider.obterChaves()).thenReturn(Map.of("outra-chave", chave.getPublic()));

        assertThatThrownBy(() -> verificador.verificar(tokenValido()))
                .isInstanceOf(JwtInvalidoException.class)
                .hasMessageContaining(KID);

        org.mockito.Mockito.verify(jwksProvider, org.mockito.Mockito.atLeastOnce()).obterChaves();
    }
}