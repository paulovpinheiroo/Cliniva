package com.cliniva.resumo;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cliniva.tenancy.ClinicaContext;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/resumo-do-dia")
@RequiredArgsConstructor
public class ResumoController {

    private final ResumoService resumoService;
    private final ClinicaContext clinicaContext;

    @GetMapping
    public ResumoDoDiaDTO obterResumo() {
        return resumoService.obterResumo(clinicaContext.obterClinicaAtual());
    }

    @PostMapping("/regenerar")
    public ResumoDoDiaDTO regenerar() {
        return resumoService.regenerar(clinicaContext.obterClinicaAtual());
    }
}