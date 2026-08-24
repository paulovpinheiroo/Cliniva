package com.cliniva.item;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.cliniva.item.dtos.CreateItemRequestDTO;
import com.cliniva.item.dtos.CreateItemResponseDTO;
import com.cliniva.item.dtos.ItemResponseDTO;
import com.cliniva.item.dtos.MovimentacaoEstoqueRequestDTO;
import com.cliniva.item.dtos.UpdateItemRequestDTO;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/items")
@RequiredArgsConstructor
public class ItemController {
    private final ItemService itemService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CreateItemResponseDTO createItem(@Valid @RequestBody CreateItemRequestDTO requestDTO) {
        return itemService.createItem(requestDTO);
    }

    @GetMapping
    public List<ItemResponseDTO> listarItems() {
        return itemService.listarItems();
    }

    @GetMapping("/{id}")
    public ItemResponseDTO buscarPorId(@PathVariable UUID id) {
        return itemService.buscarPorId(id);
    }

    @PutMapping("/{id}")
    public ItemResponseDTO atualizarItem(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateItemRequestDTO requestDTO) {
        return itemService.atualizarItem(id, requestDTO);
    }

    @PatchMapping("/{id}/estoque")
    public ItemResponseDTO movimentarEstoque(
            @PathVariable UUID id,
            @Valid @RequestBody MovimentacaoEstoqueRequestDTO requestDTO) {
        return itemService.movimentarEstoque(id, requestDTO);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletarItem(@PathVariable UUID id) {
        itemService.deletarItem(id);
    }
}
