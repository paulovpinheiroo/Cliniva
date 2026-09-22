package com.cliniva.agenda.model;

import java.io.Serializable;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class HorarioAtendimentoId implements Serializable {

    @Column(name = "clinica_id", nullable = false)
    private UUID clinicaId;
    @Column(name = "dia_semana", nullable = false)
    private Integer diaSemana;

    protected HorarioAtendimentoId() {
    }

    public HorarioAtendimentoId(UUID clinicaId, Integer diaSemana) {
        this.clinicaId = clinicaId;
        this.diaSemana = diaSemana;
    }

    public UUID getClinicaId() {
        return clinicaId;
    }

    public Integer getDiaSemana() {
        return diaSemana;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof HorarioAtendimentoId that)) {
            return false;
        }
        return java.util.Objects.equals(clinicaId, that.clinicaId)
                && java.util.Objects.equals(diaSemana, that.diaSemana);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(clinicaId, diaSemana);
    }
}