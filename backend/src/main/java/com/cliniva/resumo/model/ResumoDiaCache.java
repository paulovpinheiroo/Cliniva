package com.cliniva.resumo.model;

import java.time.Instant;
import java.time.LocalDate;

import com.cliniva.tenancy.Clinica;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity(name = "ResumoDiaCache")
@Table(name = "resumo_dia_cache")
@Getter
@Setter
@NoArgsConstructor
public class ResumoDiaCache {

    public enum Origem {
        IA,
        TEMPLATE
    }

    @EmbeddedId
    private ResumoDiaCacheId id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "clinica_id", insertable = false, updatable = false)
    private Clinica clinica;

    @Column(name = "texto", nullable = false)
    private String texto;

    @Enumerated(EnumType.STRING)
    @Column(name = "origem", nullable = false)
    private Origem origem;

    @Column(name = "tentativas", nullable = false)
    private int tentativas = 0;

    @Column(name = "gerado_em")
    private Instant geradoEm;
}