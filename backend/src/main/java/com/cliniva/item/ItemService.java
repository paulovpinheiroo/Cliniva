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
import com.cliniva.tenancy.Clinica;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ItemService {
    private final ItemRepository itemRepository;
    private final AtendimentoItemRepository atendimentoItemRepository;

    public CreateItemResponseDTO createItem(Clinica clinica, CreateItemRequestDTO requestDTO) {
        if (itemRepository.existsByNomeAndClinica(requestDTO.nome(), clinica)) {
            throw new RecursoDuplicadoException("Item com o mesmo nome já existe");
        }
        Item item = new Item();
        item.setClinica(clinica);
        item.setNome(requestDTO.nome());
        BigDecimal quantidadeInicial = requestDTO.quantidadeEmEstoque() != null
                ? requestDTO.quantidadeEmEstoque()
                : BigDecimal.ZERO;
        item.adicionarQuantidade(quantidadeInicial);
        itemRepository.save(item);
        return new CreateItemResponseDTO(item.getId(), item.getNome(), item.getQuantidadeEmEstoque());
    }

    @Transactional(readOnly = true)
    public List<ItemResponseDTO> listarItems(Clinica clinica) {
        return itemRepository.findByClinica(clinica).stream().map(this::toResponseDTO).toList();
    }

    @Transactional(readOnly = true)
    public ItemResponseDTO buscarPorId(Clinica clinica, UUID id) {
        Item item = itemRepository.findByIdAndClinica(id, clinica)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Item não encontrado"));
        return toResponseDTO(item);
    }

    @Transactional
    public ItemResponseDTO atualizarItem(Clinica clinica, UUID id, UpdateItemRequestDTO requestDTO) {
        Item item = itemRepository.findByIdAndClinica(id, clinica)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Item não encontrado"));

        if (itemRepository.existsByNomeAndIdNotAndClinica(requestDTO.nome(), id, clinica)) {
            throw new RecursoDuplicadoException("Item com o mesmo nome já existe");
        }

        item.setNome(requestDTO.nome());
        return toResponseDTO(itemRepository.save(item));
    }

    @Transactional
    public ItemResponseDTO movimentarEstoque(Clinica clinica, UUID id, MovimentacaoEstoqueRequestDTO requestDTO) {
        Item item = itemRepository.findByIdAndClinica(id, clinica)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Item não encontrado"));

        if (requestDTO.tipo() == MovimentacaoEstoqueRequestDTO.TipoMovimentacao.ENTRADA) {
            item.adicionarQuantidade(requestDTO.quantidade());
        } else {
            item.removerQuantidade(requestDTO.quantidade());
        }

        return toResponseDTO(itemRepository.save(item));
    }

    @Transactional
    public void deletarItem(Clinica clinica, UUID id) {
        if (!itemRepository.existsByIdAndClinica(id, clinica)) {
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
