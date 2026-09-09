package com.cliniva.auth;

import java.util.UUID;

import com.cliniva.tenancy.Clinica;
import com.cliniva.tenancy.Papel;

public record UsuarioPrincipal(
        UUID id,
        String supabaseUserId,
        Papel papel,
        Clinica clinica,
        String nome,
        String email) {

    public boolean ehAdmin() {
        return papel == Papel.ADMIN;
    }
}