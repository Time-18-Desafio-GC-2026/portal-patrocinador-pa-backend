package com.desafiogc.time18.entities;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "marca")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Marca {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 150)
    private String nome;

    
    @Column(nullable = false, unique = true, length = 14)
    private String cnpj;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Cota cota;

    
    @Column(nullable = false, length = 100)
    private String evento;

    
    @Column(name = "senha_padrao_hash", nullable = false, length = 100)
    private String senhaPadraoHash;

    @Column(nullable = false)
    private boolean ativa;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;

    
    @PrePersist
    void aoCriar() {
        criadoEm = Instant.now();
        atualizadoEm = criadoEm;
        if (evento == null || evento.isBlank()) {
            evento = "Planeta Atlântida 2026";
        }
    }

    
    @PreUpdate
    void aoAtualizar() {
        atualizadoEm = Instant.now();
    }
}
