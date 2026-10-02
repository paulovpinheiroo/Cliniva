package com.cliniva.common;

/**
 * Normalização de campos que recebem o mesmo dado por caminhos diferentes
 * (admin, onboarding, booking público, cadastro manual).
 *
 * <p>Precisa ser um único lugar porque o schema é sensível a isso: o índice
 * {@code uk_cliente_email_clinica} é case-sensitive, então "Maria@X.com" e
 * "maria@x.com" gravariam duas clientes — a menos que a entrada seja
 * normalizada antes de chegar no repositório.
 */
public final class Normalizador {

    private Normalizador() {
    }

    /** E-mail canônico: sem espaços nas pontas e em minúsculas. */
    public static String email(String email) {
        if (email == null || email.isBlank()) {
            return null;
        }
        return email.trim().toLowerCase();
    }

    /** Telefone só com dígitos, com DDI 55 para números brasileiros sem prefixo. */
    public static String telefone(String telefone) {
        String digitos = telefone == null ? "" : telefone.replaceAll("\\D", "");
        if (digitos.isEmpty()) {
            return telefone;
        }
        return digitos.startsWith("55") ? digitos : "55" + digitos;
    }
}
