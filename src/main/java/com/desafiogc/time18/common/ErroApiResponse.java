package com.desafiogc.time18.common;

import java.time.Instant;

/**
 * Molde (DTO) do JSON de erro devolvido pela API.
 */
public record ErroApiResponse(
        Instant dataHora,
        int status,
        String erro,
        String mensagem,
        String caminho
) {}