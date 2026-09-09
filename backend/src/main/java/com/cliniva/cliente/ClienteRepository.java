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

    @Query("SELECT c FROM Cliente c WHERE c.clinica = :clinica AND EXTRACT(MONTH FROM c.dataNascimento) = :mes")
    List<Cliente> findByDataNascimentoMesAndClinica(@Param("mes") int mes, @Param("clinica") Clinica clinica);

    @Query("SELECT c.clinica.id, COUNT(c) FROM Cliente c GROUP BY c.clinica.id")
    List<Object[]> contarPorClinica();

    long countByClinica(Clinica clinica);
}