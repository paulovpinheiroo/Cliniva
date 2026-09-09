package com.cliniva.tenancy;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.cliniva.auth.UsuarioPrincipal;
import com.cliniva.exception.AcessoNaoPermitidoException;
import com.cliniva.tenancy.dtos.TenancyDtos.CadastroOnboardingRequestDTO;
import com.cliniva.tenancy.dtos.TenancyDtos.CadastroOnboardingResponseDTO;
import com.cliniva.tenancy.dtos.TenancyDtos.MeResponseDTO;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class AuthController {

    private final OnboardingService onboardingService;
    private final ClinicaContext clinicaContext;

    @PostMapping("/api/public/onboarding")
    @ResponseStatus(HttpStatus.CREATED)
    public CadastroOnboardingResponseDTO cadastrarClinica(
            @Valid @RequestBody CadastroOnboardingRequestDTO request) {
        return onboardingService.cadastrarClinica(request);
    }

    @GetMapping("/api/me")
    public MeResponseDTO me() {
        UsuarioPrincipal principal = clinicaContext.principalAtual();
        if (principal == null) {
            throw new AcessoNaoPermitidoException("Não autenticado");
        }
        return new MeResponseDTO(principal.id(), principal.nome(), principal.email(), principal.papel(),
                principal.clinica() != null ? principal.clinica().getId() : null,
                principal.clinica() != null ? principal.clinica().getNome() : null);
    }
}