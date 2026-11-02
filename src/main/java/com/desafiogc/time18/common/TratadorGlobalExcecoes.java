package com.desafiogc.time18.common;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

/**
 * @RestControllerAdvice: Funciona como uma "rede de proteção" global para todos os Controllers.
 * Se qualquer regra de negócio falhar, ele captura a exceção e monta um JSON padronizado.
 */
@RestControllerAdvice
public class TratadorGlobalExcecoes {

    // 1. Captura nossas exceções de regras de negócio (ExcecaoApi)
    @ExceptionHandler(ExcecaoApi.class)
    public ResponseEntity<ErroApiResponse> tratarExcecaoApi(ExcecaoApi ex, HttpServletRequest request) {
        return montarResposta(ex.getStatus(), ex.getMessage(), request);
    }

    // 2. Captura erros de validação dos campos (@NotBlank, @Email nos DTOs)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErroApiResponse> tratarValidacao(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String mensagem = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(erro -> erro.getDefaultMessage())
                .orElse("Dados enviados são inválidos.");
        return montarResposta(HttpStatus.BAD_REQUEST, mensagem, request);
    }

    // 3. Captura bloqueios de permissão do Spring Security (@PreAuthorize)
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErroApiResponse> tratarAcessoNegado(AccessDeniedException ex, HttpServletRequest request) {
        return montarResposta(HttpStatus.FORBIDDEN, "Acesso negado: você não tem permissão para esta operação.", request);
    }

    // 4. Captura conflitos de banco de dados (ex: CNPJ ou E-mail duplicado)
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErroApiResponse> tratarIntegridade(DataIntegrityViolationException ex, HttpServletRequest request) {
        return montarResposta(HttpStatus.CONFLICT, "Operação conflita com registros já cadastrados no banco.", request);
    }

    private ResponseEntity<ErroApiResponse> montarResposta(HttpStatus status, String mensagem, HttpServletRequest request) {
        ErroApiResponse corpo = new ErroApiResponse(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                mensagem,
                request.getRequestURI()
        );
        return ResponseEntity.status(status).body(corpo);
    }
}