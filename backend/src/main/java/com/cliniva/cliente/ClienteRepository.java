package com.cliniva.cliente;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.cliniva.cliente.enums.ClienteStatus;

public interface ClienteRepository extends JpaRepository<Cliente, UUID> {
    Optional<Cliente> findByEmail(String email);

    Optional<Cliente> findByTelefone(String telefone);

    boolean existsByEmail(String email);

    boolean existsByTelefone(String telefone);

    List<Cliente> findByNomeContainingIgnoreCase(String nome);

    List<Cliente> findByStatus(ClienteStatus status);

    List<Cliente> findByNomeContainingIgnoreCaseAndStatus(String nome, ClienteStatus status);

    boolean existsByEmailAndIdNot(String email, UUID id);

    boolean existsByTelefoneAndIdNot(String telefone, UUID id);

    @Query("SELECT c FROM Cliente c WHERE EXTRACT(MONTH FROM c.dataNascimento) = :mes")
    List<Cliente> findByDataNascimentoMes(@Param("mes") int mes);
}