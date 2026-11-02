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

    // Armazena apenas os 14 números do CNPJ (validados antes de salvar)
    @Column(nullable = false, unique = true, length = 14)
    private String cnpj;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Cota cota;

    // Vínculo explícito com o evento (exigido no critério "Diagnóstico dos dados" da Entrega 2)
    @Column(nullable = false, length = 100)
    private String evento;

    // Senha inicial criptografada para os novos usuários desta marca
    @Column(name = "senha_padrao_hash", nullable = false, length = 100)
    private String senhaPadraoHash;

    @Column(nullable = false)
    private boolean ativa;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;

    // Executado automaticamente pelo JPA antes de dar INSERT no banco
    @PrePersist
    void aoCriar() {
        criadoEm = Instant.now();
        atualizadoEm = criadoEm;
        if (evento == null || evento.isBlank()) {
            evento = "Planeta Atlântida 2026";
        }
    }

    // Executado automaticamente pelo JPA antes de dar UPDATE no banco
    @PreUpdate
    void aoAtualizar() {
        atualizadoEm = Instant.now();
    }
}
