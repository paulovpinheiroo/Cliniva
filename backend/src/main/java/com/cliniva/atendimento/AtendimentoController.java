package com.cliniva.atendimento;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.cliniva.atendimento.dtos.AtendimentoResumoResponseDTO;
import com.cliniva.atendimento.dtos.AtendimentoResponseDTO;
import com.cliniva.atendimento.dtos.CreateAtendimentoRequestDTO;
import com.cliniva.atendimento.dtos.CreateAtendimentoResponseDTO;
import com.cliniva.atendimento.dtos.UpdateAtendimentoRequestDTO;
import com.cliniva.atendimento.dtos.UpdateStatusAtendimentoRequestDTO;
import com.cliniva.atendimento.enums.StatusAtendimento;
import com.cliniva.tenancy.ClinicaContext;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/atendimentos")
@RequiredArgsConstructor
public class AtendimentoController {
    private final AtendimentoService atendimentoService;
    private final ClinicaContext clinicaContext;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CreateAtendimentoResponseDTO createAtendimento(@Valid @RequestBody CreateAtendimentoRequestDTO requestDTO) {
        return atendimentoService.createAtendimento(clinicaContext.obterClinicaAtual(), requestDTO);
    }

    @GetMapping
    public List<AtendimentoResumoResponseDTO> listarAtendimentos(
            @RequestParam(required = false) StatusAtendimento status,
            @RequestParam(required = false) UUID clienteId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dataInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dataFim) {
        return atendimentoService.listarAtendimentos(clinicaContext.obterClinicaAtual(), status, clienteId, dataInicio,
                dataFim);
    }

    @GetMapping("/{id}")
    public AtendimentoResponseDTO buscarPorId(@PathVariable UUID id) {
        return atendimentoService.buscarPorId(clinicaContext.obterClinicaAtual(), id);
    }

    @PatchMapping("/{id}/status")
    public AtendimentoResponseDTO alterarStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateStatusAtendimentoRequestDTO requestDTO) {
        return atendimentoService.alterarStatus(clinicaContext.obterClinicaAtual(), id, requestDTO.novoStatus());
    }

    @PutMapping("/{id}")
    public AtendimentoResponseDTO atualizarAtendimento(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateAtendimentoRequestDTO requestDTO) {
        return atendimentoService.atualizarAtendimento(clinicaContext.obterClinicaAtual(), id, requestDTO);
    }
}