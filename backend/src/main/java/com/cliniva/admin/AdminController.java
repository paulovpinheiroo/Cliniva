package com.cliniva.admin;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.cliniva.admin.dtos.AdminDtos.AdicionarUsuarioRequestDTO;
import com.cliniva.admin.dtos.AdminDtos.AdicionarUsuarioResponseDTO;
import com.cliniva.admin.dtos.AdminDtos.AtualizarClinicaRequestDTO;
import com.cliniva.admin.dtos.AdminDtos.ClinicaDetalheResponseDTO;
import com.cliniva.admin.dtos.AdminDtos.CriarClinicaRequestDTO;
import com.cliniva.admin.dtos.AdminDtos.CriarClinicaResponseDTO;
import com.cliniva.admin.dtos.AdminDtos.ListarClinicaResponseDTO;
import com.cliniva.admin.dtos.AdminDtos.MetricasAdminResponseDTO;
import com.cliniva.admin.dtos.AdminDtos.ResetarSenhaResponseDTO;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/clinicas")
    public List<ListarClinicaResponseDTO> listarClinicas() {
        return adminService.listarClinicas();
    }

    @PostMapping("/clinicas")
    @ResponseStatus(HttpStatus.CREATED)
    public CriarClinicaResponseDTO criarClinica(
            @Valid @RequestBody CriarClinicaRequestDTO request) {
        return adminService.criarClinica(request);
    }

    @GetMapping("/clinicas/{id}")
    public ClinicaDetalheResponseDTO detalharClinica(@PathVariable UUID id) {
        return adminService.detalharClinica(id);
    }

    @PatchMapping("/clinicas/{id}")
    public ClinicaDetalheResponseDTO atualizarClinica(@PathVariable UUID id,
            @Valid @RequestBody AtualizarClinicaRequestDTO request) {
        return adminService.atualizarClinica(id, request);
    }

    @PostMapping("/clinicas/{id}/usuarios")
    @ResponseStatus(HttpStatus.CREATED)
    public AdicionarUsuarioResponseDTO adicionarResponsavel(@PathVariable UUID id,
            @Valid @RequestBody AdicionarUsuarioRequestDTO request) {
        return adminService.adicionarResponsavel(id, request);
    }

    @PostMapping("/usuarios/{id}/reset-senha")
    public ResetarSenhaResponseDTO resetarSenha(@PathVariable UUID id) {
        return adminService.resetarSenha(id);
    }

    @GetMapping("/metricas")
    public MetricasAdminResponseDTO metricas() {
        return adminService.metricas();
    }
}