package com.cliniva.atendimento.model;

import java.math.BigDecimal;
import java.util.UUID;

import com.cliniva.servico.Servico;

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

@Entity(name = "Atendimento_Servico")
@Table(name = "atendimento_servico", uniqueConstraints = @UniqueConstraint(name = "uk_atendimento_servico", columnNames = {
        "atendimento_id", "servico_id" }))
@Getter
@Setter
@NoArgsConstructor
public class AtendimentoServico {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(name = "valor_cobrado", nullable = false)
    private BigDecimal valorCobrado;
    @JoinColumn
    @ManyToOne
    private Atendimento atendimento;
    @JoinColumn
    @ManyToOne
    private Servico servico;

}
