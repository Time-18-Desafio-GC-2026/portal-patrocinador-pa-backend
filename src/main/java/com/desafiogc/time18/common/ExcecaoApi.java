package com.desafiogc.time18.common;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class ExcecaoApi extends RuntimeException {

    private final HttpStatus status;

    public ExcecaoApi(HttpStatus status, String mensagem) {
        super(mensagem);
        this.status = status;
    }

    
    public static ExcecaoApi dadosInvalidos(String msg) { return new ExcecaoApi(HttpStatus.BAD_REQUEST, msg); }
    public static ExcecaoApi naoAutenticado(String msg) { return new ExcecaoApi(HttpStatus.UNAUTHORIZED, msg); }
    public static ExcecaoApi acessoNegado(String msg)   { return new ExcecaoApi(HttpStatus.FORBIDDEN, msg); }
    public static ExcecaoApi naoEncontrado(String msg)  { return new ExcecaoApi(HttpStatus.NOT_FOUND, msg); }
    public static ExcecaoApi conflito(String msg)       { return new ExcecaoApi(HttpStatus.CONFLICT, msg); }
    public static ExcecaoApi erroInterno(String msg)    { return new ExcecaoApi(HttpStatus.INTERNAL_SERVER_ERROR, msg); }
}