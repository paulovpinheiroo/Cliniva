package com.cliniva.atendimento;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import com.cliniva.atendimento.enums.StatusAtendimento;
import com.cliniva.cliente.Cliente;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity(name = "Atendimento")
@Table(name = "atendimento")
@Getter
@Setter
@NoArgsConstructor
public class Atendimento {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(name = "data_criacao", nullable = false)
    private LocalDate dataCriacao;
    @Column(name = "data_atendimento", nullable = false)
    private LocalDateTime dataAtendimento;
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private StatusAtendimento status;
    @JoinColumn
    @ManyToOne
    private Cliente cliente;

    @PrePersist
    public void prePersist() {
        this.dataCriacao = LocalDate.now();
    }
}
