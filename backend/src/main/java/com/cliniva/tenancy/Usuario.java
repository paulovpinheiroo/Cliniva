package com.cliniva.tenancy;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
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

@Entity(name = "Usuario")
@Table(name = "usuario")
@Getter
@Setter
@NoArgsConstructor
public class Usuario {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(name = "supabase_user_id", unique = true)
    private String supabaseUserId;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "clinica_id")
    private Clinica clinica;
    @Enumerated(EnumType.STRING)
    @Column(name = "papel", nullable = false)
    private Papel papel;
    @Column(name = "ativo", nullable = false)
    private boolean ativo = true;
    @Column(name = "nome")
    private String nome;
    @Column(name = "email", nullable = false, unique = true)
    private String email;
    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @PrePersist
    void prePersist() {
        if (criadoEm == null) {
            criadoEm = Instant.now();
        }
    }
}