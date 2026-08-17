package com.cliniva.item;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ItemRepository extends JpaRepository<Item, UUID> {
    boolean existsByNome(String nome);

    Optional<Item> findByNome(String nome);

}
