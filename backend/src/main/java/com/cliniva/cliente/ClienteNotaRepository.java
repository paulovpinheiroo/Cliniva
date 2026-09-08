package com.cliniva.cliente;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ClienteNotaRepository extends JpaRepository<ClienteNota, UUID> {

    List<ClienteNota> findByCliente_IdOrderByCriadaEmDesc(UUID clienteId);

    boolean existsByCliente_Id(UUID clienteId);
}