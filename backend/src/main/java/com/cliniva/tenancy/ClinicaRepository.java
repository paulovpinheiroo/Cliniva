package com.cliniva.tenancy;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface ClinicaRepository extends JpaRepository<Clinica, UUID> {
    Optional<Clinica> findByNome(String nome);

    Optional<Clinica> findBySlug(String slug);

    /** Clínicas ativas são as únicas permitidas no booking público. */
    Optional<Clinica> findBySlugAndAtivaTrue(String slug);

    boolean existsBySlug(String slug);

    boolean existsByNome(String nome);

    boolean existsByNomeAndIdNot(String nome, UUID id);

    /**
     * Lock pessimista por clínica: serializa reservas, remarcações e consumo de
     * estoque dentro da mesma clínica, garantindo que o check de disponibilidade
     * e o insert ocorram sem interleaving de outras transações.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Clinica c WHERE c.id = :id")
    Optional<Clinica> findByIdParaUpdate(@Param("id") UUID id);
}
