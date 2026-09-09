package com.cliniva.tenancy.dtos;

import java.util.UUID;

import com.cliniva.tenancy.Papel;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public final class TenancyDtos {

    private TenancyDtos() {
    }

    public record CadastroOnboardingRequestDTO(
            @NotBlank(message = "Nome da clínica é obrigatório") String nomeClinica,
            @NotBlank(message = "Nome do responsável é obrigatório") String nomeResponsavel,
            @NotBlank(message = "E-mail é obrigatório") @Email(message = "E-mail inválido") String email) {
    }

    public record CadastroOnboardingResponseDTO(UUID clinicaId, String clinicaNome, UUID responsavelId) {
    }

    public record MeResponseDTO(UUID id, String nome, String email, Papel papel, UUID clinicaId,
            String clinicaNome) {
    }
}