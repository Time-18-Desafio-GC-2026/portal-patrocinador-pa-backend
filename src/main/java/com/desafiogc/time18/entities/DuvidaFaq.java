package com.desafiogc.time18.entities;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "duvida_faq")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DuvidaFaq {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 500)
    private String pergunta;

    @Column(columnDefinition = "TEXT")
    private String resposta;

    @Column(nullable = false, length = 50)
    private String categoria;

    // PENDENTE (aguardando resposta da RBS) ou RESPONDIDA
    @Column(nullable = false, length = 20)
    private String status;

    // Quando true, fica visível no FAQ e gera alerta para todos os patrocinadores!
    @Column(nullable = false)
    private boolean publica;

    // Contador incrementado a cada leitura para ordenar pelas mais acessadas
    @Column(nullable = false)
    private int visualizacoes;

    @Column(name = "autor_pergunta_id")
    private UUID autorPerguntaId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "autor_marca_id")
    private Marca autorMarca;

    @Column(name = "respondido_por", length = 150)
    private String respondidoPor;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @Column(name = "respondido_em")
    private Instant respondidoEm;

    @PrePersist
    void aoCriar() {
        criadoEm = Instant.now();
        if (status == null || status.isBlank()) {
            status = "PENDENTE";
        }
    }
}