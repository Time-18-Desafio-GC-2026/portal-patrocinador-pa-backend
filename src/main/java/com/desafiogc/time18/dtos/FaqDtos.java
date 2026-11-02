package com.desafiogc.time18.dtos;

import com.desafiogc.time18.entities.DuvidaFaq;
import jakarta.validation.constraints.NotBlank;

import java.time.Instant;
import java.util.UUID;

/**
 * DTOs para o FAQ Dinâmico (ordenado por mais acessadas) e Alertas Globais de novas respostas.
 */
public final class FaqDtos {

    // Patrocinador envia uma nova dúvida para a RBS
    public record NovaPerguntaFaqRequest(
            @NotBlank(message = "Escreva a sua dúvida.")
            String pergunta,

            @NotBlank(message = "Informe a categoria (ex: OPERACOES, COMERCIAL, CREDENCIAMENTO, MIDIA, GERAL).")
            String categoria
    ) {}

    // Admin RBS redige a resposta e publica no FAQ
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
            String mensagemAlerta // Usado no mural de alertas: "Nova resposta adicionada ao FAQ: [Pergunta]"
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