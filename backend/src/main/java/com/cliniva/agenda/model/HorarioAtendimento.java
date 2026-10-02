package com.cliniva.agenda.model;

import java.time.LocalTime;

import com.cliniva.tenancy.Clinica;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity(name = "HorarioAtendimento")
@Table(name = "horario_atendimento")
@Getter
@Setter
@NoArgsConstructor
public class HorarioAtendimento {

    @EmbeddedId
    private HorarioAtendimentoId id;

    @MapsId("clinicaId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "clinica_id", nullable = false)
    private Clinica clinica;

    @Column(name = "abertura", nullable = false)
    private LocalTime abertura;

    @Column(name = "fechamento", nullable = false)
    private LocalTime fechamento;

    @Column(name = "ativo", nullable = false)
    private boolean ativo = true;
}