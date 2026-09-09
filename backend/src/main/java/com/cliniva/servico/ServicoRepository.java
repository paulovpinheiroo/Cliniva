package com.cliniva.servico;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cliniva.tenancy.Clinica;

public interface ServicoRepository extends JpaRepository<Servico, UUID> {
    Optional<Servico> findByNome(String nome);

    boolean existsByNome(String nome);

    boolean existsByNomeAndIdNot(String nome, UUID id);

    Optional<Servico> findByIdAndClinica(UUID id, Clinica clinica);

    List<Servico> findByClinica(Clinica clinica);

    boolean existsByNomeAndClinica(String nome, Clinica clinica);

    boolean existsByNomeAndIdNotAndClinica(String nome, UUID id, Clinica clinica);
}