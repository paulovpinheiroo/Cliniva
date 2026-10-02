package com.cliniva.atendimento.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

import com.cliniva.atendimento.enums.StatusAtendimento;
import com.cliniva.cliente.Cliente;
import com.cliniva.exception.TransicaoStatusInvalidaException;

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

import com.cliniva.tenancy.Clinica;

@Entity(name = "Atendimento")
@Table(name = "atendimento")
@Getter
@Setter
@NoArgsConstructor
public class Atendimento {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "clinica_id", nullable = false)
    private Clinica clinica;
    @Setter
    @Column(name = "data_criacao", nullable = false)
    private LocalDate dataCriacao;
    @Column(name = "data_atendimento", nullable = false)
    private LocalDateTime dataAtendimento;
    @Column(name = "duracao_minutos", nullable = false)
    private Integer duracaoMinutos = 30;
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private StatusAtendimento status;
    @JoinColumn
    @ManyToOne
    private Cliente cliente;

    /**
     * Fallback quando o atendimento é criado fora do {@code AtendimentoService}
     * (importação, seed). O caminho normal define a data pelo {@code Clock}
     * injetado: {@code LocalDate.now()} sem zona usaria o fuso do container
     * (UTC no Render) e, entre 21h e 24h no Brasil, gravaria o dia seguinte.
     */
    @PrePersist
    public void prePersist() {
        if (this.dataCriacao == null) {
            this.dataCriacao = LocalDate.now(ZoneId.of("America/Sao_Paulo"));
        }
    }

    public void setStatus(StatusAtendimento status) {
        if (this.status != null && this.status == StatusAtendimento.CANCELADO) {
            throw new TransicaoStatusInvalidaException(
                    "Atendimento cancelado não pode mais ser alterado");
        }
        this.status = status;
    }
}
