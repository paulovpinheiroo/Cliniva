package com.cliniva.agenda;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.cliniva.agenda.dtos.AgendaItemDTO;
import com.cliniva.agenda.dtos.DisponibilidadeDiaDTO;
import com.cliniva.agenda.dtos.HorarioRequestDTO;
import com.cliniva.agenda.dtos.HorarioResponseDTO;
import com.cliniva.tenancy.Clinica;
import com.cliniva.tenancy.ClinicaContext;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/agenda")
@RequiredArgsConstructor
public class AgendaController {

    private final AgendaService agendaService;
    private final ClinicaContext clinicaContext;

    @GetMapping
    public List<AgendaItemDTO> listarDia(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data) {
        Clinica clinica = clinicaContext.obterClinicaAtual();
        return agendaService.listarDia(clinica, data);
    }

    @GetMapping("/disponibilidade")
    public DisponibilidadeDiaDTO disponibilidadeDia(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data,
            @RequestParam UUID servicoId) {
        Clinica clinica = clinicaContext.obterClinicaAtual();
        return agendaService.disponibilidadeDia(clinica, data, servicoId);
    }

    @GetMapping("/horarios")
    public List<HorarioResponseDTO> listarHorarios() {
        Clinica clinica = clinicaContext.obterClinicaAtual();
        return agendaService.listarHorarios(clinica);
    }

    @PutMapping("/horarios")
    @ResponseStatus(HttpStatus.OK)
    public List<HorarioResponseDTO> atualizarHorarios(
            @Valid @RequestBody List<HorarioRequestDTO> horarios) {
        Clinica clinica = clinicaContext.obterClinicaAtual();
        return agendaService.atualizarHorarios(clinica, horarios);
    }
}