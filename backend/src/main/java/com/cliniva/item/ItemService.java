package com.cliniva.item;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cliniva.atendimento.repository.AtendimentoItemRepository;
import com.cliniva.exception.RecursoDuplicadoException;
import com.cliniva.exception.RecursoEmUsoException;
import com.cliniva.exception.RecursoNaoEncontradoException;
import com.cliniva.item.dtos.CreateItemRequestDTO;
import com.cliniva.item.dtos.CreateItemResponseDTO;
import com.cliniva.item.dtos.ItemResponseDTO;
import com.cliniva.item.dtos.MovimentacaoEstoqueRequestDTO;
import com.cliniva.item.dtos.UpdateItemRequestDTO;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ItemService {
    private final ItemRepository itemRepository;
    private final AtendimentoItemRepository atendimentoItemRepository;

    public CreateItemResponseDTO createItem(CreateItemRequestDTO requestDTO) {
        if (itemRepository.existsByNome(requestDTO.nome())) {
            throw new RecursoDuplicadoException("Item com o mesmo nome já existe");
        }
        Item item = new Item();
        item.setNome(requestDTO.nome());
        BigDecimal quantidadeInicial = requestDTO.quantidadeEmEstoque() != null
                ? requestDTO.quantidadeEmEstoque()
                : BigDecimal.ZERO;
        item.adicionarQuantidade(quantidadeInicial);
        itemRepository.save(item);
        return new CreateItemResponseDTO(item.getId(), item.getNome(), item.getQuantidadeEmEstoque());
    }

    @Transactional(readOnly = true)
    public List<ItemResponseDTO> listarItems() {
        return itemRepository.findAll().stream().map(this::toResponseDTO).toList();
    }

    @Transactional(readOnly = true)
    public ItemResponseDTO buscarPorId(UUID id) {
        Item item = itemRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Item não encontrado"));
        return toResponseDTO(item);
    }

    @Transactional
    public ItemResponseDTO atualizarItem(UUID id, UpdateItemRequestDTO requestDTO) {
        Item item = itemRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Item não encontrado"));

        if (itemRepository.existsByNomeAndIdNot(requestDTO.nome(), id)) {
            throw new RecursoDuplicadoException("Item com o mesmo nome já existe");
        }

        item.setNome(requestDTO.nome());
        return toResponseDTO(itemRepository.save(item));
    }

    @Transactional
    public ItemResponseDTO movimentarEstoque(UUID id, MovimentacaoEstoqueRequestDTO requestDTO) {
        Item item = itemRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Item não encontrado"));

        if (requestDTO.tipo() == MovimentacaoEstoqueRequestDTO.TipoMovimentacao.ENTRADA) {
            item.adicionarQuantidade(requestDTO.quantidade());
        } else {
            item.removerQuantidade(requestDTO.quantidade());
        }

        return toResponseDTO(itemRepository.save(item));
    }

    @Transactional
    public void deletarItem(UUID id) {
        if (!itemRepository.existsById(id)) {
            throw new RecursoNaoEncontradoException("Item não encontrado");
        }
        if (atendimentoItemRepository.existsByItem_Id(id)) {
            throw new RecursoEmUsoException("Item possui atendimentos vinculados e não pode ser excluído");
        }
        itemRepository.deleteById(id);
    }

    private ItemResponseDTO toResponseDTO(Item item) {
        return new ItemResponseDTO(item.getId(), item.getNome(), item.getQuantidadeEmEstoque());
    }
}
