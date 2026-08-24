package com.cliniva.exception;

import java.util.List;

public record ErrorResponse(int status, String mensagem, List<String> erros) {
    public ErrorResponse(int status, String mensagem) {
        this(status, mensagem, List.of());
    }
}
