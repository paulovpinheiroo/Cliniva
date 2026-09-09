package com.cliniva.exception;

public class AcessoNaoPermitidoException extends RuntimeException {
    public AcessoNaoPermitidoException(String mensagem) {
        super(mensagem);
    }
}