package com.cliniva.servico;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ServicoRepository extends JpaRepository<Servico, UUID> {
    Optional<Servico> findByNome(String nome);

    Boolean existsByNome(String nome);

}
