package com.cliniva.security.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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

/**
 * Cobre o binding do ADMIN master: o seed da migration 03 cria o usuário com
 * supabase_user_id nulo, então o filtro precisa vincular pelo e-mail do JWT.
 */
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

    @Test
    void deveAutenticarAdminMasterSemClinica() throws Exception {
        String supabaseUserId = UUID.randomUUID().toString();
        Usuario usuario = new Usuario();
        org.springframework.test.util.ReflectionTestUtils.setField(usuario, "id", UUID.randomUUID());
        usuario.setSupabaseUserId(supabaseUserId);
        usuario.setPapel(Papel.ADMIN);
        usuario.setAtivo(true);
        usuario.setNome("Administrador Cliniva");
        usuario.setEmail("admin@cliniva.com");
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
        UsuarioPrincipal principal = (UsuarioPrincipal) authentication.getPrincipal();
        assertThat(principal.papel()).isEqualTo(Papel.ADMIN);
        assertThat(principal.clinica()).isNull();
    }

    /**
     * O seed da migration 03 cria o ADMIN master com supabase_user_id nulo.
     * O filtro precisa vincular pelo e-mail do JWT — sem isso o admin master
     * nunca consegue entrar em /admin.
     */
    @Test
    void deveVincularAdminMasterPeloEmailQuandoNaoHaSupabaseUserId() throws Exception {
        String supabaseUserId = UUID.randomUUID().toString();
        Usuario admin = new Usuario();
        org.springframework.test.util.ReflectionTestUtils.setField(admin, "id", UUID.randomUUID());
        admin.setSupabaseUserId(null);
        admin.setPapel(Papel.ADMIN);
        admin.setAtivo(true);
        admin.setNome("Administrador Cliniva");
        admin.setEmail("admin@cliniva.com");

        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn(supabaseUserId);
        when(claims.get("email", String.class)).thenReturn("admin@cliniva.com");
        when(verificador.verificar(anyString())).thenReturn(claims);
        when(usuarioRepository.findBySupabaseUserId(supabaseUserId)).thenReturn(Optional.empty());
        when(usuarioRepository.findByEmailIgnoreCase("admin@cliniva.com")).thenReturn(Optional.of(admin));

        SecurityContextHolder.clearContext();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer token-valido");

        filtro.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        // vínculo persistido no banco
        verify(usuarioRepository).save(admin);
        assertThat(admin.getSupabaseUserId()).isEqualTo(supabaseUserId);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        UsuarioPrincipal principal = (UsuarioPrincipal) authentication.getPrincipal();
        assertThat(principal.papel()).isEqualTo(Papel.ADMIN);
        assertThat(principal.supabaseUserId()).isEqualTo(supabaseUserId);
    }

    @Test
    void naoDeveVincularPorEmailQuandoJaExisteSupabaseUserId() throws Exception {
        String supabaseUserId = UUID.randomUUID().toString();
        Usuario admin = new Usuario();
        org.springframework.test.util.ReflectionTestUtils.setField(admin, "id", UUID.randomUUID());
        admin.setSupabaseUserId("outro-id");
        admin.setPapel(Papel.ADMIN);
        admin.setAtivo(true);
        admin.setEmail("admin@cliniva.com");

        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn(supabaseUserId);
        when(claims.get("email", String.class)).thenReturn("admin@cliniva.com");
        when(verificador.verificar(anyString())).thenReturn(claims);
        when(usuarioRepository.findBySupabaseUserId(supabaseUserId)).thenReturn(Optional.empty());
        when(usuarioRepository.findByEmailIgnoreCase("admin@cliniva.com")).thenReturn(Optional.of(admin));

        SecurityContextHolder.clearContext();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer token-valido");

        filtro.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        // não sobrescreve um vínculo existente
        verify(usuarioRepository, never()).save(any(Usuario.class));
        assertThat(admin.getSupabaseUserId()).isEqualTo("outro-id");
    }

    @Test
    void naoDeveAutenticarQuandoEmailNaoExisteNoBanco() throws Exception {
        String supabaseUserId = UUID.randomUUID().toString();
        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn(supabaseUserId);
        when(claims.get("email", String.class)).thenReturn("desconhecido@email.com");
        when(verificador.verificar(anyString())).thenReturn(claims);
        when(usuarioRepository.findBySupabaseUserId(supabaseUserId)).thenReturn(Optional.empty());
        when(usuarioRepository.findByEmailIgnoreCase("desconhecido@email.com"))
                .thenReturn(Optional.empty());

        SecurityContextHolder.clearContext();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer token-valido");

        filtro.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }
}