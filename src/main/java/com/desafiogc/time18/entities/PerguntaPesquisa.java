package com.desafiogc.time18.entities;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "pergunta_pesquisa")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PerguntaPesquisa {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "marca_id", nullable = false)
    private Marca marca;

    
    @Column(name = "numero_pergunta", nullable = false)
    private Integer numeroPergunta;

    @Column(name = "texto_pergunta", nullable = false, length = 500)
    private String textoPergunta;

    @Column(name = "autor_nome", nullable = false, length = 150)
    private String autorNome;

    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;

    @PrePersist
    @PreUpdate
    void aoSalvar() {
        atualizadoEm = Instant.now();
    }
}