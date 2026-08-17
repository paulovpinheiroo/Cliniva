package com.cliniva.atendimento;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.cliniva.atendimento.dtos.CreateAtendimentoRequestDTO;
import com.cliniva.atendimento.dtos.CreateAtendimentoRequestDTO.ServicoSelecionadoDTO;
import com.cliniva.atendimento.dtos.CreateAtendimentoRequestDTO.ServicoSelecionadoDTO.ItemUsadoDTO;
import com.cliniva.atendimento.dtos.CreateAtendimentoResponseDTO;
import com.cliniva.atendimento.dtos.CreateAtendimentoResponseDTO.ServicoRealizadoDTO;
import com.cliniva.atendimento.dtos.CreateAtendimentoResponseDTO.ServicoRealizadoDTO.ItemUsadoRealDTO;
import com.cliniva.atendimento.enums.StatusAtendimento;
import com.cliniva.atendimento.model.Atendimento;
import com.cliniva.atendimento.model.AtendimentoItem;
import com.cliniva.atendimento.model.AtendimentoServico;
import com.cliniva.atendimento.repository.AtendimentoItemRepository;
import com.cliniva.atendimento.repository.AtendimentoRepository;
import com.cliniva.atendimento.repository.AtendimentoServicoRepository;
import com.cliniva.cliente.Cliente;
import com.cliniva.cliente.ClienteRepository;
import com.cliniva.item.Item;
import com.cliniva.item.ItemRepository;
import com.cliniva.servico.Servico;
import com.cliniva.servico.ServicoRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AtendimentoService {
    private final AtendimentoRepository atendimentoRepository;
    private final AtendimentoServicoRepository atendimentoServicoRepository;
    private final AtendimentoItemRepository atendimentoItemRepository;
    private final ClienteRepository clienteRepository;
    private final ServicoRepository servicoRepository;
    private final ItemRepository itemRepository;

    @Transactional
    public CreateAtendimentoResponseDTO createAtendimento(CreateAtendimentoRequestDTO requestDTO) {
        Cliente cliente = clienteRepository.findById(requestDTO.clienteId())
                .orElseThrow(() -> new IllegalArgumentException("Cliente não encontrado"));

        Atendimento atendimento = new Atendimento();
        atendimento.setCliente(cliente);
        atendimento.setDataAtendimento(requestDTO.dataAtendimento());
        atendimento.setStatus(StatusAtendimento.AGENDADO);
        atendimentoRepository.save(atendimento);

        List<ServicoRealizadoDTO> servicosRealizados = new ArrayList<>();

        for (ServicoSelecionadoDTO servicoSelecionado : requestDTO.servicos()) {
            Servico servicoEncontrado = servicoRepository.findById(servicoSelecionado.servicoId())
                    .orElseThrow(() -> new IllegalArgumentException("Serviço não encontrado"));

            if (atendimentoServicoRepository.existsByAtendimentoAndServico(atendimento, servicoEncontrado)) {
                throw new IllegalArgumentException("Serviço já adicionado a este atendimento.");
            }

            AtendimentoServico atendimentoServico = new AtendimentoServico();
            atendimentoServico.setAtendimento(atendimento);
            atendimentoServico.setServico(servicoEncontrado);
            atendimentoServico.setValorCobrado(servicoEncontrado.getValor());
            atendimentoServicoRepository.save(atendimentoServico);

            List<ItemUsadoRealDTO> itensRealizados = new ArrayList<>();

            for (ItemUsadoDTO itemUsadoDTO : servicoSelecionado.itensExtras()) {
                Item itemEncontrado = itemRepository.findById(itemUsadoDTO.itemId())
                        .orElseThrow(() -> new IllegalArgumentException("Item não encontrado"));

                BigDecimal quantidadeNova = itemUsadoDTO.quantidade();

                Optional<AtendimentoItem> existente = atendimentoItemRepository
                        .findByAtendimentoServicoAndItem(atendimentoServico, itemEncontrado);

                AtendimentoItem atendimentoItem;
                if (existente.isPresent()) {
                    atendimentoItem = existente.get();
                    atendimentoItem.setQuantidadeUsada(atendimentoItem.getQuantidadeUsada().add(quantidadeNova));
                } else {
                    atendimentoItem = new AtendimentoItem();
                    atendimentoItem.setAtendimentoServico(atendimentoServico);
                    atendimentoItem.setItem(itemEncontrado);
                    atendimentoItem.setQuantidadeUsada(quantidadeNova);
                }

                // delta = só o que chegou agora, nunca o total acumulado
                itemEncontrado.removerQuantidade(quantidadeNova);
                itemRepository.save(itemEncontrado);
                atendimentoItemRepository.save(atendimentoItem);

                itensRealizados.add(new ItemUsadoRealDTO(itemEncontrado.getId(), atendimentoItem.getQuantidadeUsada()));
            }

            servicosRealizados.add(new ServicoRealizadoDTO(
                    servicoEncontrado.getId(),
                    atendimentoServico.getValorCobrado(),
                    itensRealizados));
        }

        return new CreateAtendimentoResponseDTO(
                atendimento.getId(),
                cliente.getId(),
                atendimento.getDataAtendimento(),
                atendimento.getDataCriacao(),
                atendimento.getStatus(),
                servicosRealizados);
    }
}