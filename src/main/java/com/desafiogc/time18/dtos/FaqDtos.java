package com.desafiogc.time18.dtos;

import com.desafiogc.time18.entities.DuvidaFaq;
import jakarta.validation.constraints.NotBlank;

import java.time.Instant;
import java.util.UUID;


public final class FaqDtos {

    
    public record NovaPerguntaFaqRequest(
            @NotBlank(message = "Escreva a sua dúvida.")
            String pergunta,

            @NotBlank(message = "Informe a categoria (ex: OPERACOES, COMERCIAL, CREDENCIAMENTO, MIDIA, GERAL).")
            String categoria
    ) {}

    
    public record ResponderFaqRequest(
            @NotBlank(message = "Escreva a resposta oficial da RBS.")
            String resposta,

            boolean publicarParaTodos
    ) {}

    public record FaqResponse(
            UUID id,
            String pergunta,
            String resposta,
            String categoria,
            String status,
            boolean publica,
            int visualizacoes,
            String respondidoPor,
            Instant criadoEm,
            Instant respondidoEm,
            String mensagemAlerta 
    ) {
        public static FaqResponse de(DuvidaFaq f) {
            String alerta = f.isPublica() && "RESPONDIDA".equals(f.getStatus())
                    ? "Nova resposta adicionada ao FAQ: " + f.getPergunta()
                    : null;
            return new FaqResponse(
                    f.getId(),
                    f.getPergunta(),
                    f.getResposta(),
                    f.getCategoria(),
                    f.getStatus(),
                    f.isPublica(),
                    f.getVisualizacoes(),
                    f.getRespondidoPor(),
                    f.getCriadoEm(),
                    f.getRespondidoEm(),
                    alerta
            );
        }
    }
}