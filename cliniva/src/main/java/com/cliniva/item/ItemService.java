package com.cliniva.item;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;

import com.cliniva.item.dtos.CreateItemRequestDTO;
import com.cliniva.item.dtos.CreateItemResponseDTO;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ItemService {
    private final ItemRepository itemRepository;

    public CreateItemResponseDTO createItem(CreateItemRequestDTO requestDTO) {
        if (itemRepository.existsByNome(requestDTO.nome())) {
            throw new IllegalArgumentException("Item com o mesmo nome já existe");
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
}
