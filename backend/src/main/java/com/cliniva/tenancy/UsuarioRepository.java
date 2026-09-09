package com.cliniva.tenancy;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioRepository extends JpaRepository<Usuario, UUID> {
    Optional<Usuario> findBySupabaseUserId(String supabaseUserId);

    Optional<Usuario> findByEmail(String email);

    List<Usuario> findByClinica_IdOrderByNomeAsc(UUID clinicaId);

    boolean existsByEmail(String email);

    boolean existsByEmailAndIdNot(String email, UUID id);
}