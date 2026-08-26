package com.cliniva.servico;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cliniva.exception.RecursoDuplicadoException;
import com.cliniva.exception.RecursoEmUsoException;
import com.cliniva.exception.RecursoNaoEncontradoException;
import com.cliniva.atendimento.repository.AtendimentoServicoRepository;
import com.cliniva.servico.dtos.CreateServicoRequestDTO;
import com.cliniva.servico.dtos.CreateServicoResponseDTO;
import com.cliniva.servico.dtos.ServicoResponseDTO;
import com.cliniva.servico.dtos.UpdateServicoRequestDTO;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ServicoService {
    private final ServicoRepository servicoRepository;
    private final AtendimentoServicoRepository atendimentoServicoRepository;

    public CreateServicoResponseDTO createServico(CreateServicoRequestDTO requestDTO) {
        if (servicoRepository.existsByNome(requestDTO.nome())) {
            throw new RecursoDuplicadoException("Serviço com nome já existe");
        }
        Servico servico = new Servico();
        servico.setNome(requestDTO.nome());
        servico.setDescricao(requestDTO.descricao());
        servico.setValor(requestDTO.valor());
        servicoRepository.save(servico);
        return new CreateServicoResponseDTO(servico.getId(), servico.getNome(), servico.getDescricao(),
                servico.getValor());
    }

    @Transactional(readOnly = true)
    public List<ServicoResponseDTO> listarServicos() {
        return servicoRepository.findAll().stream().map(this::toResponseDTO).toList();
    }

    @Transactional(readOnly = true)
    public ServicoResponseDTO buscarPorId(UUID id) {
        Servico servico = servicoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Serviço não encontrado"));
        return toResponseDTO(servico);
    }

    @Transactional
    public ServicoResponseDTO atualizarServico(UUID id, UpdateServicoRequestDTO requestDTO) {
        Servico servico = servicoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Serviço não encontrado"));

        if (servicoRepository.existsByNomeAndIdNot(requestDTO.nome(), id)) {
            throw new RecursoDuplicadoException("Serviço com nome já existe");
        }

        servico.setNome(requestDTO.nome());
        servico.setDescricao(requestDTO.descricao());
        servico.setValor(requestDTO.valor());

        return toResponseDTO(servicoRepository.save(servico));
    }

    @Transactional
    public void deletarServico(UUID id) {
        if (!servicoRepository.existsById(id)) {
            throw new RecursoNaoEncontradoException("Serviço não encontrado");
        }
        if (atendimentoServicoRepository.existsByServico_Id(id)) {
            throw new RecursoEmUsoException("Serviço possui atendimentos vinculados e não pode ser excluído");
        }
        servicoRepository.deleteById(id);
    }

    private ServicoResponseDTO toResponseDTO(Servico servico) {
        return new ServicoResponseDTO(servico.getId(), servico.getNome(), servico.getDescricao(),
                servico.getValor());
    }

}
