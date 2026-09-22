package com.cliniva.tenancy;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ClinicaRepository extends JpaRepository<Clinica, UUID> {
    Optional<Clinica> findByNome(String nome);

    Optional<Clinica> findBySlug(String slug);

    boolean existsBySlug(String slug);

    boolean existsByNome(String nome);

    boolean existsByNomeAndIdNot(String nome, UUID id);
}