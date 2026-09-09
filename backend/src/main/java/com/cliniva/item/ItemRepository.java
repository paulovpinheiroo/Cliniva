package com.cliniva.item;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cliniva.tenancy.Clinica;

public interface ItemRepository extends JpaRepository<Item, UUID> {
    Optional<Item> findByIdAndClinica(UUID id, Clinica clinica);

    boolean existsByIdAndClinica(UUID id, Clinica clinica);

    List<Item> findByClinica(Clinica clinica);

    boolean existsByNomeAndClinica(String nome, Clinica clinica);

    boolean existsByNomeAndIdNotAndClinica(String nome, UUID id, Clinica clinica);
}