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
import com.cliniva.tenancy.Clinica;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ServicoService {
    private final ServicoRepository servicoRepository;
    private final AtendimentoServicoRepository atendimentoServicoRepository;

    public CreateServicoResponseDTO createServico(Clinica clinica, CreateServicoRequestDTO requestDTO) {
        if (servicoRepository.existsByNomeAndClinica(requestDTO.nome(), clinica)) {
            throw new RecursoDuplicadoException("Serviço com nome já existe");
        }
        Servico servico = new Servico();
        servico.setClinica(clinica);
        servico.setNome(requestDTO.nome());
        servico.setDescricao(requestDTO.descricao());
        servico.setValor(requestDTO.valor());
        servicoRepository.save(servico);
        return new CreateServicoResponseDTO(servico.getId(), servico.getNome(), servico.getDescricao(),
                servico.getValor());
    }

    @Transactional(readOnly = true)
    public List<ServicoResponseDTO> listarServicos(Clinica clinica) {
        return servicoRepository.findByClinica(clinica).stream().map(this::toResponseDTO).toList();
    }

    @Transactional(readOnly = true)
    public ServicoResponseDTO buscarPorId(Clinica clinica, UUID id) {
        Servico servico = servicoRepository.findByIdAndClinica(id, clinica)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Serviço não encontrado"));
        return toResponseDTO(servico);
    }

    @Transactional
    public ServicoResponseDTO atualizarServico(Clinica clinica, UUID id, UpdateServicoRequestDTO requestDTO) {
        Servico servico = servicoRepository.findByIdAndClinica(id, clinica)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Serviço não encontrado"));

        if (servicoRepository.existsByNomeAndIdNotAndClinica(requestDTO.nome(), id, clinica)) {
            throw new RecursoDuplicadoException("Serviço com nome já existe");
        }

        servico.setNome(requestDTO.nome());
        servico.setDescricao(requestDTO.descricao());
        servico.setValor(requestDTO.valor());

        return toResponseDTO(servicoRepository.save(servico));
    }

    @Transactional
    public void deletarServico(Clinica clinica, UUID id) {
        if (!servicoRepository.existsByIdAndClinica(id, clinica)) {
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
