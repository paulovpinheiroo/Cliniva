package com.cliniva.item;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.cliniva.exception.EstoqueInsuficienteException;

class ItemTest {

    private Item itemComEstoque(String quantidadeInicial) {
        Item item = new Item();
        item.setNome("Cera Corporal");
        item.adicionarQuantidade(new BigDecimal(quantidadeInicial));
        return item;
    }

    @Test
    @DisplayName("novo item inicia com estoque zero")
    void novoItemIniciaComEstoqueZero() {
        assertThat(new Item().getQuantidadeEmEstoque()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void deveAdicionarQuantidadeAoEstoque() {
        Item item = itemComEstoque("10");

        item.adicionarQuantidade(new BigDecimal("5"));

        assertThat(item.getQuantidadeEmEstoque()).isEqualByComparingTo(new BigDecimal("15"));
    }

    @Test
    void deveRejeitarAdicaoDeQuantidadeNegativa() {
        Item item = itemComEstoque("10");

        assertThatThrownBy(() -> item.adicionarQuantidade(new BigDecimal("-1")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void deveRemoverQuantidadeQuandoHaEstoqueSuficiente() {
        Item item = itemComEstoque("10");

        item.removerQuantidade(new BigDecimal("4"));

        assertThat(item.getQuantidadeEmEstoque()).isEqualByComparingTo(new BigDecimal("6"));
    }

    @Test
    @DisplayName("não deve remover quando o estoque é menor que a quantidade pedida")
    void naoDeveRemoverEstoqueInsuficiente() {
        Item item = itemComEstoque("2");

        assertThatThrownBy(() -> item.removerQuantidade(new BigDecimal("5")))
                .isInstanceOf(EstoqueInsuficienteException.class)
                .hasMessageContaining("Cera Corporal")
                .hasMessageContaining("2")
                .hasMessageContaining("5");

        assertThat(item.getQuantidadeEmEstoque()).isEqualByComparingTo(new BigDecimal("2"));
    }

    @Test
    void naoDeveRemoverQuantidadeZeroOuNegativa() {
        Item item = itemComEstoque("10");

        assertThatThrownBy(() -> item.removerQuantidade(BigDecimal.ZERO))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> item.removerQuantidade(new BigDecimal("-3")))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
