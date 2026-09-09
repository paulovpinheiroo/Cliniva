package com.cliniva.item;

import java.math.BigDecimal;
import java.util.UUID;

import com.cliniva.exception.EstoqueInsuficienteException;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import com.cliniva.tenancy.Clinica;

@Entity(name = "Item")
@Table(name = "item")
@NoArgsConstructor
@Getter
public class Item {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "clinica_id", nullable = false)
    private Clinica clinica;
    @Column(name = "nome", nullable = false, unique = true)
    private String nome;
    @Column(name = "quantidade", nullable = false)
    private BigDecimal quantidadeEmEstoque = BigDecimal.ZERO;

    public void setClinica(Clinica clinica) {
        this.clinica = clinica;
    }

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
        if (quantidade.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Quantidade a remover deve ser positiva");
        }
        if (this.quantidadeEmEstoque.compareTo(quantidade) < 0) {
            throw new EstoqueInsuficienteException(
                    "Estoque insuficiente para '" + nome + "': disponível " + quantidadeEmEstoque + ", solicitado "
                            + quantidade);
        }
        this.quantidadeEmEstoque = this.quantidadeEmEstoque.subtract(quantidade);
    }

}
