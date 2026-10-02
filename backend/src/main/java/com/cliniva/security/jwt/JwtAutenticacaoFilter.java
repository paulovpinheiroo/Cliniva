package com.cliniva.security.jwt;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.cliniva.auth.UsuarioPrincipal;
import com.cliniva.tenancy.Papel;
import com.cliniva.tenancy.Usuario;
import com.cliniva.tenancy.UsuarioRepository;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAutenticacaoFilter extends OncePerRequestFilter {

    private final SupabaseJwtVerificador verificador;
    private final UsuarioRepository usuarioRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String cabecalho = request.getHeader("Authorization");

        if (cabecalho != null && cabecalho.startsWith("Bearer ")) {
            String token = cabecalho.substring(7);
            try {
                Claims claims = verificador.verificar(token);
                Optional<Usuario> usuario = localizarUsuario(claims);
                if (usuario.isPresent() && autenticavel(usuario.get())) {
                    autenticar(usuario.get(), token);
                }
            } catch (RuntimeException ex) {
                // Token inválido é esperado (expirado, assinatura diferente),
                // então não polui o log com stack trace. Erros inesperados
                // ficam em debug para diagnóstico.
                log.debug("Token recusado na autenticação: {}", ex.getMessage());
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Resolve o usuário pelo id do Supabase (subject). Se ainda não houver
     * vínculo — caso do ADMIN master criado pelo seed da migration 03, cujo
     * supabase_user_id nasce nulo —, vincula pelo e-mail do próprio JWT
     * verificado. Isso só ocorre para e-mails já cadastrados no banco.
     */
    private Optional<Usuario> localizarUsuario(Claims claims) {
        Optional<Usuario> porSupabaseId = usuarioRepository.findBySupabaseUserId(claims.getSubject());
        if (porSupabaseId.isPresent()) {
            return porSupabaseId;
        }
        String email = claims.get("email", String.class);
        if (email == null || email.isBlank()) {
            return Optional.empty();
        }
        Optional<Usuario> porEmail = usuarioRepository.findByEmailIgnoreCase(email);
        porEmail.ifPresent(usuario -> {
            if (usuario.getSupabaseUserId() == null) {
                usuario.setSupabaseUserId(claims.getSubject());
                usuarioRepository.save(usuario);
            }
        });
        return porEmail;
    }

    private boolean autenticavel(Usuario usuario) {
        if (!usuario.isAtivo()) {
            return false;
        }
        if (usuario.getPapel() == Papel.ADMIN) {
            return true;
        }
        return usuario.getClinica() != null && usuario.getClinica().isAtiva();
    }

    private void autenticar(Usuario usuario, String token) {
        UsuarioPrincipal principal = new UsuarioPrincipal(usuario.getId(), usuario.getSupabaseUserId(),
                usuario.getPapel(), usuario.getClinica(), usuario.getNome(), usuario.getEmail());
        var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + usuario.getPapel().name()));
        var authentication = new UsernamePasswordAuthenticationToken(principal, token, authorities);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
