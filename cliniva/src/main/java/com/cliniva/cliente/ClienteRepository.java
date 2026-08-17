package com.cliniva.cliente;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ClienteRepository extends JpaRepository<Cliente, UUID> {
    Optional<Cliente> findByEmail(String email);

    Optional<Cliente> findByTelefone(String telefone);

    boolean existsByEmail(String email);

    boolean existsByTelefone(String telefone);
}
