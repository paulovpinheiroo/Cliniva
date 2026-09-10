package com.cliniva.security.jwt;

import java.io.IOException;
import java.util.List;

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
                usuarioRepository.findBySupabaseUserId(claims.getSubject()).ifPresent(usuario -> {
                    if (autenticavel(usuario)) {
                        autenticar(usuario, token);
                    }
                });
            } catch (RuntimeException ex) {
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
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