package com.cliniva.atendimento.model;

import java.math.BigDecimal;
import java.util.UUID;

import com.cliniva.item.Item;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity(name = "Atendimento_Item")
@Table(name = "atendimento_item", uniqueConstraints = @UniqueConstraint(name = "uk_atendimento_item", columnNames = {
        "atendimento_servico_id", "item_id" }))
@Getter
@Setter
@NoArgsConstructor
public class AtendimentoItem {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @JoinColumn
    @ManyToOne
    private AtendimentoServico atendimentoServico;
    @JoinColumn
    @ManyToOne
    private Item item;
    @Column(name = "quantidade_usada", nullable = false)
    private BigDecimal quantidadeUsada;
}
