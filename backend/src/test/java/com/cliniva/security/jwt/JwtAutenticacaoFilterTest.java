package com.cliniva.security.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.cliniva.auth.UsuarioPrincipal;
import com.cliniva.tenancy.Clinica;
import com.cliniva.tenancy.Papel;
import com.cliniva.tenancy.Usuario;
import com.cliniva.tenancy.UsuarioRepository;

import io.jsonwebtoken.Claims;

@ExtendWith(MockitoExtension.class)
class JwtAutenticacaoFilterTest {

    @Mock
    private SupabaseJwtVerificador verificador;

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private JwtAutenticacaoFilter filtro;

    private Clinica clinica() {
        Clinica clinica = new Clinica();
        org.springframework.test.util.ReflectionTestUtils.setField(clinica, "id", UUID.randomUUID());
        org.springframework.test.util.ReflectionTestUtils.setField(clinica, "nome", "Clínica Teste");
        clinica.setAtiva(true);
        return clinica;
    }

    private Usuario usuario(String supabaseUserId, Papel papel, boolean ativo) {
        Usuario usuario = new Usuario();
        org.springframework.test.util.ReflectionTestUtils.setField(usuario, "id", UUID.randomUUID());
        usuario.setSupabaseUserId(supabaseUserId);
        usuario.setClinica(clinica());
        usuario.setPapel(papel);
        usuario.setAtivo(ativo);
        usuario.setNome("Maria");
        usuario.setEmail("maria@email.com");
        return usuario;
    }

    private Claims claims(String subject) {
        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn(subject);
        return claims;
    }

    @Test
    void deveAutenticarUsuarioValido() throws Exception {
        String supabaseUserId = UUID.randomUUID().toString();
        Usuario usuario = usuario(supabaseUserId, Papel.OWNER, true);
        Claims claims = claims(supabaseUserId);
        when(verificador.verificar(anyString())).thenReturn(claims);
        when(usuarioRepository.findBySupabaseUserId(supabaseUserId))
                .thenReturn(Optional.of(usuario));

        SecurityContextHolder.clearContext();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer token-valido");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filtro.doFilter(request, response, new MockFilterChain());

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(authentication.getPrincipal()).isInstanceOf(UsuarioPrincipal.class);
        UsuarioPrincipal principal = (UsuarioPrincipal) authentication.getPrincipal();
        assertThat(principal.supabaseUserId()).isEqualTo(supabaseUserId);
        assertThat(principal.papel()).isEqualTo(Papel.OWNER);
        assertThat(authentication.getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_OWNER");
    }

    @Test
    void naoDeveAutenticarUsuarioDesativado() throws Exception {
        String supabaseUserId = UUID.randomUUID().toString();
        Usuario usuario = usuario(supabaseUserId, Papel.OWNER, false);
        Claims claims = claims(supabaseUserId);
        when(verificador.verificar(anyString())).thenReturn(claims);
        when(usuarioRepository.findBySupabaseUserId(supabaseUserId))
                .thenReturn(Optional.of(usuario));

        SecurityContextHolder.clearContext();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer token-valido");

        filtro.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void deveIgnorarRequisicaoSemCabecalho() throws Exception {
        SecurityContextHolder.clearContext();
        MockHttpServletRequest request = new MockHttpServletRequest();

        filtro.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void deveLimparContextoQuandoTokenInvalido() throws Exception {
        when(verificador.verificar(anyString()))
                .thenThrow(new JwtInvalidoException("Token inválido"));

        SecurityContextHolder.clearContext();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer token-invalido");

        filtro.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }
}