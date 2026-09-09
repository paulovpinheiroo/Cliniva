package com.cliniva.security.jwt;

public class JwtInvalidoException extends RuntimeException {
    public JwtInvalidoException(String mensagem) {
        super(mensagem);
    }
}