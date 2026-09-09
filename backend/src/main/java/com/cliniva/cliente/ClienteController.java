package com.cliniva.cliente;

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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.cliniva.cliente.dtos.ClienteHistoricoResponseDTO;
import com.cliniva.cliente.dtos.ClienteResponseDTO;
import com.cliniva.cliente.dtos.CreateClienteNotaRequestDTO;
import com.cliniva.cliente.dtos.CreateClienteRequestDTO;
import com.cliniva.cliente.dtos.CreateClienteResponseDTO;
import com.cliniva.cliente.dtos.NotaResponseDTO;
import com.cliniva.cliente.dtos.UpdateClienteRequestDTO;
import com.cliniva.cliente.enums.ClienteStatus;
import com.cliniva.tenancy.ClinicaContext;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/clientes")
@RequiredArgsConstructor
public class ClienteController {
    private final ClienteService clienteService;
    private final ClinicaContext clinicaContext;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CreateClienteResponseDTO createCliente(
                    @Valid @RequestBody CreateClienteRequestDTO createClienteRequestDTO) {
        return clienteService.createCliente(clinicaContext.obterClinicaAtual(), createClienteRequestDTO);
    }

    @GetMapping
    public List<ClienteResponseDTO> listarClientes(
                    @RequestParam(required = false) String nome,
                    @RequestParam(required = false) ClienteStatus status) {
        return clienteService.listarClientes(clinicaContext.obterClinicaAtual(), nome, status);
    }

    @GetMapping("/aniversariantes")
    public List<ClienteResponseDTO> aniversariantes(
                    @RequestParam(required = false) Integer mes) {
        return clienteService.aniversariantes(clinicaContext.obterClinicaAtual(), mes);
    }

    @GetMapping("/{id}")
    public ClienteResponseDTO buscarPorId(@PathVariable UUID id) {
        return clienteService.buscarPorId(clinicaContext.obterClinicaAtual(), id);
    }

    @GetMapping("/{id}/historico")
    public ClienteHistoricoResponseDTO historico(@PathVariable UUID id) {
        return clienteService.historicoCliente(clinicaContext.obterClinicaAtual(), id);
    }

    @GetMapping("/{id}/notas")
    public List<NotaResponseDTO> listarNotas(@PathVariable UUID id) {
        return clienteService.listarNotas(clinicaContext.obterClinicaAtual(), id);
    }

    @PostMapping("/{id}/notas")
    @ResponseStatus(HttpStatus.CREATED)
    public NotaResponseDTO criarNota(
                    @PathVariable UUID id,
                    @Valid @RequestBody CreateClienteNotaRequestDTO requestDTO) {
        return clienteService.criarNota(clinicaContext.obterClinicaAtual(), id, requestDTO);
    }

    @DeleteMapping("/{id}/notas/{notaId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletarNota(@PathVariable UUID id, @PathVariable UUID notaId) {
        clienteService.deletarNota(clinicaContext.obterClinicaAtual(), id, notaId);
    }

    @PutMapping("/{id}")
    public ClienteResponseDTO atualizarCliente(
                    @PathVariable UUID id,
                    @Valid @RequestBody UpdateClienteRequestDTO requestDTO) {
        return clienteService.atualizarCliente(clinicaContext.obterClinicaAtual(), id, requestDTO);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletarCliente(@PathVariable UUID id) {
        clienteService.deletarCliente(clinicaContext.obterClinicaAtual(), id);
    }

}