package com.cliniva.servico;

import java.math.BigDecimal;
import java.util.UUID;

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
import lombok.Setter;

import com.cliniva.tenancy.Clinica;

@Entity(name = "Servico")
@Table(name = "servico")
@Getter
@Setter
@NoArgsConstructor
public class Servico {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "clinica_id", nullable = false)
    private Clinica clinica;
    @Column(name = "nome", nullable = false, unique = true)
    private String nome;
    @Column(name = "descricao")
    private String descricao;
    @Column(name = "valor", nullable = false)
    private BigDecimal valor;
}
