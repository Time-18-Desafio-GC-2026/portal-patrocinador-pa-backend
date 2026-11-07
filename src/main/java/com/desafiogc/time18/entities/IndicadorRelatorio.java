package com.desafiogc.time18.entities;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "indicador_relatorio")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IndicadorRelatorio {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "marca_id", nullable = false)
    private Marca marca;

    @Column(nullable = false, length = 100)
    private String evento;

    
    @Column(nullable = false, length = 50)
    private String secao;

    
    @Column(name = "nome_indicador", nullable = false, length = 150)
    private String nomeIndicador;

    
    @Column(name = "valor_numerico", precision = 15, scale = 2)
    private BigDecimal valorNumerico;

    
    @Column(name = "valor_texto", length = 500)
    private String valorTexto;

    
    @Column(name = "fonte_dados", nullable = false, length = 150)
    private String fonteDados;

    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;

    @PrePersist
    @PreUpdate
    void aoSalvar() {
        atualizadoEm = Instant.now();
        if (evento == null || evento.isBlank()) {
            evento = "Planeta Atlântida 2026";
        }
    }
}