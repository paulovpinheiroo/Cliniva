package com.cliniva.tenancy;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity(name = "Clinica")
@Table(name = "clinica")
@Getter
@Setter
@NoArgsConstructor
public class Clinica {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(name = "nome", nullable = false)
    private String nome;
    @Column(name = "ativa", nullable = false)
    private boolean ativa = true;
    @Column(name = "criada_em", nullable = false, updatable = false)
    private Instant criadaEm;

    @PrePersist
    void prePersist() {
        if (criadaEm == null) {
            criadaEm = Instant.now();
        }
    }
}