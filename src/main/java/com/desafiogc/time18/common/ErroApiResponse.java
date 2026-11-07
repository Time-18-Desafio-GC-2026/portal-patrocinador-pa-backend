package com.desafiogc.time18.common;

import java.time.Instant;


public record ErroApiResponse(
        Instant dataHora,
        int status,
        String erro,
        String mensagem,
        String caminho
) {}