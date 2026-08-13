package com.cliniva.item;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity(name = "Item")
@Table(name = "item")
@NoArgsConstructor
@Getter
public class Item {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(name = "nome", nullable = false, unique = true)
    private String nome;
    @Column(name = "quantidade", nullable = false)
    private BigDecimal quantidadeEmEstoque = BigDecimal.ZERO;

    public void setNome(String nome) {
        this.nome = nome;
    }

    public void adicionarQuantidade(BigDecimal quantidade) {
        if (quantidade.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Quantidade não pode ser negativa");
        }
        this.quantidadeEmEstoque = this.quantidadeEmEstoque.add(quantidade);
    }

    public void removerQuantidade(BigDecimal quantidade) {
        if (this.quantidadeEmEstoque.compareTo(quantidade) < 0) {
            throw new IllegalStateException("Estoque insuficiente");
        }
        this.quantidadeEmEstoque = this.quantidadeEmEstoque.subtract(quantidade);
    }

}
