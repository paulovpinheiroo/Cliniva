package com.cliniva.exception;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.cliniva.booking.RateLimitExcedidoException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleNaoEncontrado(RecursoNaoEncontradoException ex) {
        return new ErrorResponse(HttpStatus.NOT_FOUND.value(), ex.getMessage());
    }

    @ExceptionHandler({ LimiteDeGeracoesExcedidoException.class, RateLimitExcedidoException.class })
    @ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
    public ErrorResponse handleLimiteDeGeracoes(RuntimeException ex) {
        return new ErrorResponse(HttpStatus.TOO_MANY_REQUESTS.value(), ex.getMessage());
    }

    @ExceptionHandler(AcessoNaoPermitidoException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ErrorResponse handleAcessoNaoPermitido(AcessoNaoPermitidoException ex) {
        return new ErrorResponse(HttpStatus.FORBIDDEN.value(), ex.getMessage());
    }

    @ExceptionHandler(SupabaseIndisponivelException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public ErrorResponse handleSupabaseIndisponivel(SupabaseIndisponivelException ex) {
        return new ErrorResponse(HttpStatus.SERVICE_UNAVAILABLE.value(), ex.getMessage());
    }

    @ExceptionHandler({ RecursoDuplicadoException.class, EstoqueInsuficienteException.class,
            TransicaoStatusInvalidaException.class, RecursoEmUsoException.class, HorarioIndisponivelException.class })
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleConflito(RuntimeException ex) {
        return new ErrorResponse(HttpStatus.CONFLICT.value(), ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleArgumentoInvalido(IllegalArgumentException ex) {
        return new ErrorResponse(HttpStatus.BAD_REQUEST.value(), ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleValidacao(MethodArgumentNotValidException ex) {
        var erros = ex.getBindingResult().getFieldErrors().stream()
                .map(erro -> erro.getField() + ": " + erro.getDefaultMessage())
                .toList();
        return new ErrorResponse(HttpStatus.BAD_REQUEST.value(), "Requisição inválida", erros);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleConstraintViolation(ConstraintViolationException ex) {
        var erros = ex.getConstraintViolations().stream()
                .map(violacao -> violacao.getPropertyPath() + ": " + violacao.getMessage())
                .toList();
        return new ErrorResponse(HttpStatus.BAD_REQUEST.value(), "Requisição inválida", erros);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleTipoIncompativel(MethodArgumentTypeMismatchException ex) {
        String mensagem = "Parâmetro '" + ex.getName() + "' recebeu valor com tipo inválido";
        return new ErrorResponse(HttpStatus.BAD_REQUEST.value(), mensagem);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleCorpoIlegivel(HttpMessageNotReadableException ex) {
        return new ErrorResponse(HttpStatus.BAD_REQUEST.value(), "Corpo da requisição malformado ou ilegível");
    }

    /**
     * Violação de unicidade/CHECK no banco (e-mail duplicado, estoque negativo,
     * sobreposição de agenda) vira 409 com mensagem útil em vez de 500.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleIntegridade(DataIntegrityViolationException ex) {
        return new ErrorResponse(HttpStatus.CONFLICT.value(),
                "Não foi possível concluir: já existe um registro conflitante ou um valor ficou inválido.");
    }

    /**
     * Rede de segurança: a aplicação não deve derrubar a requisição por
     * exceção não mapeada, e o cliente precisa distinguir "deu ruim no
     * servidor" de "sem permissão" — o que era impossível antes.
     *
     * <p>Não é um {@code @ExceptionHandler(Exception.class)} genérico de
     * propósito: ele engoliria também as exceções padrão do Spring MVC
     * (405 método não permitido, 415 mídia não suportada) e as transformaria
     * em 500, escondendo erro do cliente. As exceções do MVC têm handlers
     * próprios no {@code ResponseEntityExceptionResolver} e ficam como estão.
     *
     * <p>O que sobra aqui (NPE, bug de lógica) ia antes para a página de erro
     * do Tomcat — HTML sem detalhe, e Worse: o dispatch de erro voltava pela
     * cadeia de segurança sem contexto e virava 401.
     */
    @ExceptionHandler(NullPointerException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleNulo(NullPointerException ex, HttpServletRequest request) {
        log.error("Erro não tratado em {} {}", request.getMethod(), request.getRequestURI(), ex);
        return new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Erro interno inesperado. Tente novamente ou contate o suporte.");
    }
}
