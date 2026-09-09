package com.cliniva.tenancy;

import java.util.UUID;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.cliniva.auth.UsuarioPrincipal;
import com.cliniva.exception.AcessoNaoPermitidoException;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

/**
 * Resolve a clínica "efetiva" de cada requisição:
 * <ul>
 * <li>OWNER → a própria clínica do usuário;</li>
 * <li>ADMIN → visa a clínica informada no header {@code X-Clinica}
 * (modo suporte);</li>
 * <li>sem autenticação (desenvolvimento) → a clínica padrão semeada pela
 * migration do Supabase, preservando o comportamento mono-clínica do MVP.</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class ClinicaContext {

    private final ClinicaRepository clinicaRepository;

    public Clinica obterClinicaAtual() {
        UsuarioPrincipal principal = principalAtual();
        if (principal != null) {
            if (principal.ehAdmin()) {
                return clinicaEmModoSuporte();
            }
            if (principal.clinica() == null) {
                throw new AcessoNaoPermitidoException("Usuário não vinculado a uma clínica");
            }
            return clinicaValida(principal.clinica().getId());
        }
        return clinicaPadrao();
    }

    public boolean modoSuporte() {
        UsuarioPrincipal principal = principalAtual();
        return principal != null && principal.ehAdmin()
                && request().getHeader("X-Clinica") != null;
    }

    public boolean ehAdmin() {
        UsuarioPrincipal principal = principalAtual();
        return principal != null && principal.ehAdmin();
    }

    public UsuarioPrincipal principalAtual() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UsuarioPrincipal principal) {
            return principal;
        }
        return null;
    }

    private Clinica clinicaEmModoSuporte() {
        String clinicaId = request().getHeader("X-Clinica");
        if (clinicaId == null || clinicaId.isBlank()) {
            throw new AcessoNaoPermitidoException(
                    "Administradores devem informar a clínica no header X-Clinica");
        }
        return clinicaValida(UUID.fromString(clinicaId));
    }

    private Clinica clinicaValida(UUID id) {
        Clinica clinica = clinicaRepository.findById(id)
                .orElseThrow(() -> new AcessoNaoPermitidoException("Clínica não encontrada"));
        if (!clinica.isAtiva()) {
            throw new AcessoNaoPermitidoException("Clínica desativada");
        }
        return clinica;
    }

    private Clinica clinicaPadrao() {
        return clinicaRepository.findByNome("Clínica Padrão")
                .orElseThrow(() -> new AcessoNaoPermitidoException("Nenhuma clínica disponível"));
    }

    private HttpServletRequest request() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
            return attrs.getRequest();
        }
        throw new AcessoNaoPermitidoException("Contexto de requisição indisponível");
    }
}