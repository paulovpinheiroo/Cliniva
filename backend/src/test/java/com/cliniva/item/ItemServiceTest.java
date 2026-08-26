package com.cliniva.item;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cliniva.atendimento.repository.AtendimentoItemRepository;
import com.cliniva.exception.RecursoDuplicadoException;
import com.cliniva.exception.RecursoEmUsoException;
import com.cliniva.exception.RecursoNaoEncontradoException;
import com.cliniva.item.dtos.CreateItemRequestDTO;
import com.cliniva.item.dtos.MovimentacaoEstoqueRequestDTO;
import com.cliniva.item.dtos.UpdateItemRequestDTO;

@ExtendWith(MockitoExtension.class)
class ItemServiceTest {

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private AtendimentoItemRepository atendimentoItemRepository;

    @InjectMocks
    private ItemService itemService;

    private Item item(UUID id, String nome, String estoque) {
        Item item = new Item();
        item.setNome(nome);
        if (estoque != null) {
            item.adicionarQuantidade(new BigDecimal(estoque));
        }
        if (id != null) {
            org.springframework.test.util.ReflectionTestUtils.setField(item, "id", id);
        }
        return item;
    }

    @Test
    void deveCriarItemComEstoqueInformado() {
        when(itemRepository.existsByNome("Sérum Vitamina C")).thenReturn(false);
        when(itemRepository.save(any(Item.class)))
                .thenAnswer(invocacao -> invocacao.getArgument(0));

        var resposta = itemService.createItem(
                new CreateItemRequestDTO("Sérum Vitamina C", new BigDecimal("10")));

        assertThat(resposta.quantidadeEmEstoque()).isEqualByComparingTo("10");
    }

    @Test
    void deveCriarItemSemEstoqueIniciandoEmZero() {
        when(itemRepository.existsByNome("Sérum Vitamina C")).thenReturn(false);
        when(itemRepository.save(any(Item.class)))
                .thenAnswer(invocacao -> invocacao.getArgument(0));

        var resposta = itemService.createItem(
                new CreateItemRequestDTO("Sérum Vitamina C", null));

        assertThat(resposta.quantidadeEmEstoque()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void naoDeveCriarItemComNomeDuplicado() {
        when(itemRepository.existsByNome("Sérum Vitamina C")).thenReturn(true);

        assertThatThrownBy(() -> itemService.createItem(
                new CreateItemRequestDTO("Sérum Vitamina C", null)))
                .isInstanceOf(RecursoDuplicadoException.class);
    }

    @Test
    void entradaDeveSomarAoEstoque() {
        UUID id = UUID.randomUUID();
        Item item = item(id, "Sérum Vitamina C", "10");
        when(itemRepository.findById(id)).thenReturn(Optional.of(item));
        when(itemRepository.save(any(Item.class)))
                .thenAnswer(invocacao -> invocacao.getArgument(0));

        var resposta = itemService.movimentarEstoque(id,
                new MovimentacaoEstoqueRequestDTO(
                        MovimentacaoEstoqueRequestDTO.TipoMovimentacao.ENTRADA, new BigDecimal("5")));

        assertThat(resposta.quantidadeEmEstoque()).isEqualByComparingTo("15");
    }

    @Test
    void saidaDeveSubtrairDoEstoque() {
        UUID id = UUID.randomUUID();
        Item item = item(id, "Sérum Vitamina C", "10");
        when(itemRepository.findById(id)).thenReturn(Optional.of(item));
        when(itemRepository.save(any(Item.class)))
                .thenAnswer(invocacao -> invocacao.getArgument(0));

        var resposta = itemService.movimentarEstoque(id,
                new MovimentacaoEstoqueRequestDTO(
                        MovimentacaoEstoqueRequestDTO.TipoMovimentacao.SAIDA, new BigDecimal("4")));

        assertThat(resposta.quantidadeEmEstoque()).isEqualByComparingTo("6");
    }

    @Test
    void movimentacaoDeItemInexistenteRetornaErro() {
        UUID id = UUID.randomUUID();
        when(itemRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> itemService.movimentarEstoque(id,
                new MovimentacaoEstoqueRequestDTO(
                        MovimentacaoEstoqueRequestDTO.TipoMovimentacao.ENTRADA, BigDecimal.ONE)))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void naoDeveRenomearComNomeDeOutroItem() {
        UUID id = UUID.randomUUID();
        when(itemRepository.findById(id)).thenReturn(Optional.of(item(id, "Sérum", null)));
        when(itemRepository.existsByNomeAndIdNot("Cera", id)).thenReturn(true);

        assertThatThrownBy(() -> itemService.atualizarItem(
                id, new UpdateItemRequestDTO("Cera")))
                .isInstanceOf(RecursoDuplicadoException.class);
    }

    @Test
    void deveRenomearItem() {
        UUID id = UUID.randomUUID();
        when(itemRepository.findById(id)).thenReturn(Optional.of(item(id, "Sérum", null)));
        when(itemRepository.existsByNomeAndIdNot("Sérum Facial", id)).thenReturn(false);
        when(itemRepository.save(any(Item.class)))
                .thenAnswer(invocacao -> invocacao.getArgument(0));

        var resposta = itemService.atualizarItem(id, new UpdateItemRequestDTO("Sérum Facial"));

        assertThat(resposta.nome()).isEqualTo("Sérum Facial");
    }

    @Test
    void naoDeveDeletarItemUsadoEmAtendimentos() {
        UUID id = UUID.randomUUID();
        when(itemRepository.existsById(id)).thenReturn(true);
        when(atendimentoItemRepository.existsByItem_Id(id)).thenReturn(true);

        assertThatThrownBy(() -> itemService.deletarItem(id))
                .isInstanceOf(RecursoEmUsoException.class);

        verify(itemRepository, never()).deleteById(any());
    }

    @Test
    void naoDeveDeletarItemInexistente() {
        UUID id = UUID.randomUUID();
        when(itemRepository.existsById(id)).thenReturn(false);

        assertThatThrownBy(() -> itemService.deletarItem(id))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }
}
