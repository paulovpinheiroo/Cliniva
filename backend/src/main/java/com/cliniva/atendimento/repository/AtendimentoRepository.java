package com.cliniva.atendimento.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import com.cliniva.atendimento.model.Atendimento;
import com.cliniva.tenancy.Clinica;

public interface AtendimentoRepository
        extends JpaRepository<Atendimento, UUID>, JpaSpecificationExecutor<Atendimento> {

    Optional<Atendimento> findByIdAndClinica(UUID id, Clinica clinica);

    List<Atendimento> findByCliente_Id(UUID clienteId);

    boolean existsByCliente_Id(UUID clienteId);

    @Query("SELECT a.clinica.id, COUNT(a) FROM Atendimento a GROUP BY a.clinica.id")
    List<Object[]> contarPorClinica();

    long countByClinica(Clinica clinica);
}