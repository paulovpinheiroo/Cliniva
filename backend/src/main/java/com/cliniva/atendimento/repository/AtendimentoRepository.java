package com.cliniva.atendimento.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.cliniva.atendimento.model.Atendimento;
import com.cliniva.tenancy.Clinica;

public interface AtendimentoRepository
        extends JpaRepository<Atendimento, UUID>, JpaSpecificationExecutor<Atendimento> {

    Optional<Atendimento> findByIdAndClinica(UUID id, Clinica clinica);

    List<Atendimento> findByCliente_Id(UUID clienteId);

    /**
     * Intervalo semiaberto [inicio, fim): um atendimento exatamente à meia-noite
     * pertence apenas ao dia seguinte (sem duplicidade entre dias).
     */
    @Query("SELECT a FROM Atendimento a WHERE a.clinica = :clinica "
            + "AND a.dataAtendimento >= :inicio AND a.dataAtendimento < :fim")
    List<Atendimento> findPorIntervalo(@Param("clinica") Clinica clinica,
            @Param("inicio") LocalDateTime inicio,
            @Param("fim") LocalDateTime fim);

    List<Atendimento> findByClinicaAndDataAtendimentoBefore(Clinica clinica,
            LocalDateTime data);

    boolean existsByCliente_Id(UUID clienteId);

    @Query("SELECT a.clinica.id, COUNT(a) FROM Atendimento a GROUP BY a.clinica.id")
    List<Object[]> contarPorClinica();

    long countByClinica(Clinica clinica);
}
