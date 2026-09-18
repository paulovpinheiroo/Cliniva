package com.cliniva.exception;

public class LimiteDeGeracoesExcedidoException extends RuntimeException {
    public LimiteDeGeracoesExcedidoException(String mensagem) {
        super(mensagem);
    }
}