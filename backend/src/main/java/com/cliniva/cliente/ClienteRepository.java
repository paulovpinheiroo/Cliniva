package com.cliniva.cliente;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.cliniva.cliente.enums.ClienteStatus;
import com.cliniva.tenancy.Clinica;

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

    Optional<Cliente> findByIdAndClinica(UUID id, Clinica clinica);

    boolean existsByIdAndClinica(UUID id, Clinica clinica);

    boolean existsByEmailAndClinica(String email, Clinica clinica);

    boolean existsByTelefoneAndClinica(String telefone, Clinica clinica);

    boolean existsByEmailAndIdNotAndClinica(String email, UUID id, Clinica clinica);

    boolean existsByTelefoneAndIdNotAndClinica(String telefone, UUID id, Clinica clinica);

    List<Cliente> findByClinica(Clinica clinica);

    List<Cliente> findByClinicaAndNomeContainingIgnoreCase(Clinica clinica, String nome);

    List<Cliente> findByClinicaAndStatus(Clinica clinica, ClienteStatus status);

    List<Cliente> findByClinicaAndNomeContainingIgnoreCaseAndStatus(Clinica clinica, String nome,
            ClienteStatus status);

    @Query("SELECT c FROM Cliente c WHERE EXTRACT(MONTH FROM c.dataNascimento) = :mes")
    List<Cliente> findByDataNascimentoMes(@Param("mes") int mes);

    @Query("SELECT c FROM Cliente c WHERE c.clinica = :clinica AND EXTRACT(MONTH FROM c.dataNascimento) = :mes")
    List<Cliente> findByDataNascimentoMesAndClinica(@Param("mes") int mes, @Param("clinica") Clinica clinica);
}