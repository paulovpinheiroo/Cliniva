package com.cliniva.exception;

public class SupabaseIndisponivelException extends RuntimeException {
    public SupabaseIndisponivelException(String mensagem) {
        super(mensagem);
    }
}