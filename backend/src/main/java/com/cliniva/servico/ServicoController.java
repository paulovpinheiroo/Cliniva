package com.cliniva.servico;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.cliniva.servico.dtos.CreateServicoRequestDTO;
import com.cliniva.servico.dtos.CreateServicoResponseDTO;
import com.cliniva.servico.dtos.ServicoResponseDTO;
import com.cliniva.servico.dtos.UpdateServicoRequestDTO;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/servicos")
@RequiredArgsConstructor
public class ServicoController {
    private final ServicoService servicoService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CreateServicoResponseDTO createServico(@Valid @RequestBody CreateServicoRequestDTO requestDTO) {
        return servicoService.createServico(requestDTO);
    }

    @GetMapping
    public List<ServicoResponseDTO> listarServicos() {
        return servicoService.listarServicos();
    }

    @GetMapping("/{id}")
    public ServicoResponseDTO buscarPorId(@PathVariable UUID id) {
        return servicoService.buscarPorId(id);
    }

    @PutMapping("/{id}")
    public ServicoResponseDTO atualizarServico(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateServicoRequestDTO requestDTO) {
        return servicoService.atualizarServico(id, requestDTO);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletarServico(@PathVariable UUID id) {
        servicoService.deletarServico(id);
    }
}
