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

    // Seção do relatório: MIDIA, PESQUISA_ATIVACAO, REDES_SOCIAIS, COMPROVACAO
    @Column(nullable = false, length = 50)
    private String secao;

    // Nome da métrica (ex: "Inserções TV", "Alcance Digital", "Aprovação do Público %")
    @Column(name = "nome_indicador", nullable = false, length = 150)
    private String nomeIndicador;

    // Número usado pelo Front-end para desenhar os gráficos de barras/linhas
    @Column(name = "valor_numerico", precision = 15, scale = 2)
    private BigDecimal valorNumerico;

    // Observação ou resultado qualitativo
    @Column(name = "valor_texto", length = 500)
    private String valorTexto;

    // Explica a origem do indicador (Critério de Aceite de "Modelo do relatório" da Entrega 2)
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