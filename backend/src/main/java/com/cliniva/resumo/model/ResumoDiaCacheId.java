package com.cliniva.resumo.model;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
public class ResumoDiaCacheId implements Serializable {

    @Column(name = "clinica_id")
    private UUID clinicaId;

    @Column(name = "data")
    private LocalDate data;

    public ResumoDiaCacheId(UUID clinicaId, LocalDate data) {
        this.clinicaId = clinicaId;
        this.data = data;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ResumoDiaCacheId that)) {
            return false;
        }
        return Objects.equals(clinicaId, that.clinicaId) && Objects.equals(data, that.data);
    }

    @Override
    public int hashCode() {
        return Objects.hash(clinicaId, data);
    }
}