package com.cliniva.admin.dtos;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.cliniva.tenancy.Papel;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public final class AdminDtos {

    private AdminDtos() {
    }

    public record CriarClinicaRequestDTO(
            @NotBlank(message = "Nome da clínica é obrigatório") String nome,
            @NotBlank(message = "E-mail do responsável é obrigatório") @Email(message = "E-mail inválido") String emailResponsavel,
            @NotBlank(message = "Nome do responsável é obrigatório") String nomeResponsavel) {
    }

    public record AtualizarClinicaRequestDTO(String nome, Boolean ativa) {
    }

    public record AdicionarUsuarioRequestDTO(
            @NotBlank(message = "E-mail é obrigatório") @Email(message = "E-mail inválido") String email,
            @NotBlank(message = "Nome é obrigatório") String nome) {
    }

    public record CriarClinicaResponseDTO(UUID clinicaId, String clinicaNome, String emailResponsavel,
            String senhaTemporaria) {
    }

    public record ListarClinicaResponseDTO(UUID id, String nome, boolean ativa, Instant criadaEm,
            long totalResponsaveis) {
    }

    public record UsuarioResponseDTO(UUID id, String nome, String email, Papel papel, boolean ativo) {
    }

    public record AdicionarUsuarioResponseDTO(UUID id, String email, String nome, Papel papel,
            String senhaTemporaria) {
    }

    public record ResetarSenhaResponseDTO(String email, String senhaTemporaria) {
    }

    public record ClinicaDetalheResponseDTO(UUID id, String nome, boolean ativa, Instant criadaEm,
            long totalClientes, long totalAtendimentos, BigDecimal receita,
            List<UsuarioResponseDTO> responsaveis) {
    }

    public record MetricasAdminResponseDTO(long totalClinicas, long totalClientes, long totalAtendimentos,
            List<MetricaClinicaDTO> porClinica) {
    }

    public record MetricaClinicaDTO(String clinicaId, String clinicaNome, long clientes, long atendimentos,
            BigDecimal receita) {
    }
}