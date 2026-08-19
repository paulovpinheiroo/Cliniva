package com.cliniva.servico;

import org.springframework.stereotype.Service;

import com.cliniva.exception.RecursoNaoEncontradoException;
import com.cliniva.servico.dtos.CreateServicoRequestDTO;
import com.cliniva.servico.dtos.CreateServicoResponseDTO;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ServicoService {
    private final ServicoRepository servicoRepository;

    public CreateServicoResponseDTO createServico(CreateServicoRequestDTO requestDTO) {
        if (servicoRepository.existsByNome(requestDTO.nome())) {
            throw new RecursoNaoEncontradoException("Serviço com nome já existe");
        }
        Servico servico = new Servico();
        servico.setNome(requestDTO.nome());
        servico.setDescricao(requestDTO.descricao());
        servico.setValor(requestDTO.valor());
        servicoRepository.save(servico);
        return new CreateServicoResponseDTO(servico.getId(), servico.getNome(), servico.getDescricao(),
                servico.getValor());
    }

}
