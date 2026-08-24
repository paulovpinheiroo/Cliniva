package com.cliniva.atendimento;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cliniva.atendimento.dtos.AtendimentoResumoResponseDTO;
import com.cliniva.atendimento.dtos.AtendimentoResponseDTO;
import com.cliniva.atendimento.dtos.CreateAtendimentoRequestDTO;
import com.cliniva.atendimento.dtos.UpdateAtendimentoRequestDTO;
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
import com.cliniva.exception.RecursoDuplicadoException;
import com.cliniva.exception.RecursoNaoEncontradoException;
import com.cliniva.exception.TransicaoStatusInvalidaException;
import com.cliniva.item.Item;
import com.cliniva.item.ItemRepository;
import com.cliniva.servico.Servico;
import com.cliniva.servico.ServicoRepository;

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
                                .orElseThrow(() -> new RecursoNaoEncontradoException("Cliente não encontrado"));

                Atendimento atendimento = new Atendimento();
                atendimento.setCliente(cliente);
                atendimento.setDataAtendimento(requestDTO.dataAtendimento());
                atendimento.setStatus(StatusAtendimento.AGENDADO);
                atendimentoRepository.save(atendimento);

                List<ServicoRealizadoDTO> servicosRealizados = new ArrayList<>();

                for (ServicoSelecionadoDTO servicoSelecionado : requestDTO.servicos()) {
                        Servico servicoEncontrado = servicoRepository.findById(servicoSelecionado.servicoId())
                                        .orElseThrow(() -> new RecursoNaoEncontradoException("Serviço não encontrado"));

                        if (atendimentoServicoRepository.existsByAtendimentoAndServico(atendimento,
                                        servicoEncontrado)) {
                                throw new RecursoDuplicadoException("Serviço já adicionado a este atendimento.");
                        }

                        AtendimentoServico atendimentoServico = new AtendimentoServico();
                        atendimentoServico.setAtendimento(atendimento);
                        atendimentoServico.setServico(servicoEncontrado);
                        atendimentoServico.setValorCobrado(servicoEncontrado.getValor());
                        atendimentoServicoRepository.save(atendimentoServico);

                        List<ItemUsadoRealDTO> itensRealizados = new ArrayList<>();

                        for (ItemUsadoDTO itemUsadoDTO : servicoSelecionado.itensExtras()) {
                                Item itemEncontrado = itemRepository.findById(itemUsadoDTO.itemId())
                                                .orElseThrow(() -> new RecursoNaoEncontradoException(
                                                                "Item não encontrado"));

                                BigDecimal quantidadeNova = itemUsadoDTO.quantidade();

                                Optional<AtendimentoItem> existente = atendimentoItemRepository
                                                .findByAtendimentoServicoAndItem(atendimentoServico, itemEncontrado);

                                AtendimentoItem atendimentoItem;
                                if (existente.isPresent()) {
                                        atendimentoItem = existente.get();
                                        atendimentoItem.setQuantidadeUsada(
                                                        atendimentoItem.getQuantidadeUsada().add(quantidadeNova));
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

                                itensRealizados.add(new ItemUsadoRealDTO(itemEncontrado.getId(),
                                                atendimentoItem.getQuantidadeUsada()));
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

        @Transactional
        public AtendimentoResponseDTO alterarStatus(UUID id, StatusAtendimento novoStatus) {
                Atendimento atendimento = atendimentoRepository.findById(id)
                                .orElseThrow(() -> new RecursoNaoEncontradoException("Atendimento não encontrado"));

                // CANCELADO é o único estado terminal
                if (atendimento.getStatus() == StatusAtendimento.CANCELADO) {
                        throw new TransicaoStatusInvalidaException("Atendimento cancelado não pode mais ser alterado");
                }
                if (novoStatus == atendimento.getStatus()) {
                        throw new TransicaoStatusInvalidaException("Atendimento já está com status " + novoStatus);
                }

                // devolve estoque SOMENTE se o serviço nunca aconteceu;
                // CONCLUIDO → CANCELADO é correção de digitação e o produto foi usado de
                // verdade
                if (novoStatus == StatusAtendimento.CANCELADO
                                && atendimento.getStatus() == StatusAtendimento.AGENDADO) {
                        devolverEstoque(atendimento);
                }

                atendimento.setStatus(novoStatus);
                atendimentoRepository.save(atendimento);

                return buscarPorId(id);
        }

        @Transactional
        public AtendimentoResponseDTO atualizarAtendimento(UUID id, UpdateAtendimentoRequestDTO requestDTO) {
                Atendimento atendimento = atendimentoRepository.findById(id)
                                .orElseThrow(() -> new RecursoNaoEncontradoException("Atendimento não encontrado"));

                if (atendimento.getStatus() != StatusAtendimento.AGENDADO) {
                        throw new TransicaoStatusInvalidaException(
                                        "Somente atendimentos com status AGENDADO podem ser alterados");
                }

                Cliente cliente = clienteRepository.findById(requestDTO.clienteId())
                                .orElseThrow(() -> new RecursoNaoEncontradoException("Cliente não encontrado"));

                atendimento.setCliente(cliente);
                atendimento.setDataAtendimento(requestDTO.dataAtendimento());
                atendimentoRepository.save(atendimento);

                return buscarPorId(id);
        }

        // cancelamento devolve ao estoque o que foi consumido nos serviços do
        // atendimento
        private void devolverEstoque(Atendimento atendimento) {
                List<AtendimentoItem> itensUsados = atendimentoItemRepository
                                .findByAtendimentoServico_Atendimento(atendimento);

                for (AtendimentoItem atendimentoItem : itensUsados) {
                        Item item = atendimentoItem.getItem();
                        item.adicionarQuantidade(atendimentoItem.getQuantidadeUsada());
                        itemRepository.save(item);
                }
        }

        @Transactional(readOnly = true)
        public List<AtendimentoResumoResponseDTO> listarAtendimentos(StatusAtendimento status, UUID clienteId,
                        LocalDateTime dataInicio, LocalDateTime dataFim) {
                Specification<Atendimento> specification = AtendimentoSpecifications.comFiltros(status, clienteId,
                                dataInicio, dataFim);

                return atendimentoRepository.findAll(specification).stream()
                                .map(this::toResumoResponseDTO)
                                .toList();
        }

        @Transactional(readOnly = true)
        public AtendimentoResponseDTO buscarPorId(UUID id) {
                Atendimento atendimento = atendimentoRepository.findById(id)
                                .orElseThrow(() -> new RecursoNaoEncontradoException("Atendimento não encontrado"));

                List<AtendimentoResponseDTO.ServicoRealizadoDTO> servicos = atendimentoServicoRepository
                                .findByAtendimento(atendimento).stream()
                                .map(this::toServicoRealizadoDTO)
                                .toList();

                return new AtendimentoResponseDTO(
                                atendimento.getId(),
                                atendimento.getCliente().getId(),
                                atendimento.getCliente().getNome(),
                                atendimento.getDataAtendimento(),
                                atendimento.getDataCriacao(),
                                atendimento.getStatus(),
                                calcularValorTotal(atendimento),
                                servicos);
        }

        private AtendimentoResumoResponseDTO toResumoResponseDTO(Atendimento atendimento) {
                return new AtendimentoResumoResponseDTO(
                                atendimento.getId(),
                                atendimento.getCliente().getNome(),
                                atendimento.getDataAtendimento(),
                                atendimento.getStatus(),
                                calcularValorTotal(atendimento));
        }

        private AtendimentoResponseDTO.ServicoRealizadoDTO toServicoRealizadoDTO(
                        AtendimentoServico atendimentoServico) {
                List<AtendimentoResponseDTO.ServicoRealizadoDTO.ItemUsadoRealDTO> itensUsados = atendimentoItemRepository
                                .findByAtendimentoServico(atendimentoServico).stream()
                                .map(atendimentoItem -> new AtendimentoResponseDTO.ServicoRealizadoDTO.ItemUsadoRealDTO(
                                                atendimentoItem.getItem().getId(),
                                                atendimentoItem.getItem().getNome(),
                                                atendimentoItem.getQuantidadeUsada()))
                                .toList();

                return new AtendimentoResponseDTO.ServicoRealizadoDTO(
                                atendimentoServico.getServico().getId(),
                                atendimentoServico.getServico().getNome(),
                                atendimentoServico.getValorCobrado(),
                                itensUsados);
        }

        private BigDecimal calcularValorTotal(Atendimento atendimento) {
                return atendimentoServicoRepository.findByAtendimento(atendimento).stream()
                                .map(AtendimentoServico::getValorCobrado)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);
        }
}