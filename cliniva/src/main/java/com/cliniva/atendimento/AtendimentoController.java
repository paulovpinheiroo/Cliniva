package com.cliniva.atendimento;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.cliniva.atendimento.dtos.CreateAtendimentoRequestDTO;
import com.cliniva.atendimento.dtos.CreateAtendimentoResponseDTO;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class AtendimentoController {
    private final AtendimentoService atendimentoService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CreateAtendimentoResponseDTO createAtendimento(@RequestBody CreateAtendimentoRequestDTO requestDTO) {
        return atendimentoService.createAtendimento(requestDTO);
    }
}
