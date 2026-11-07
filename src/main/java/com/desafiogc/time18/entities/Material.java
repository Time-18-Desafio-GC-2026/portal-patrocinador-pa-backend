package com.desafiogc.time18.entities;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "material")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Material {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "marca_id", nullable = false)
    private Marca marca;

    @Column(nullable = false, length = 150)
    private String titulo;

    @Column(length = 500)
    private String descricao;

    
    @Column(name = "aba_destino", nullable = false, length = 30)
    private String abaDestino;

    
    @Column(name = "tipo_material", nullable = false, length = 30)
    private String tipoMaterial;

    
    @Column(name = "caminho_storage", length = 500)
    private String caminhoStorage;

    
    @Column(name = "url_externa", length = 500)
    private String urlExterna;

    
    @Column(nullable = false, length = 30)
    private String status;

    
    @Column(name = "autor_id", nullable = false)
    private UUID autorId;

    @Column(name = "autor_nome", nullable = false, length = 150)
    private String autorNome;

    @Enumerated(EnumType.STRING)
    @Column(name = "autor_perfil", nullable = false, length = 20)
    private Perfil autorPerfil;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;

    @PrePersist
    void aoCriar() {
        criadoEm = Instant.now();
        atualizadoEm = criadoEm;
        if (status == null || status.isBlank()) {
            status = "ENVIADO";
        }
    }

    @PreUpdate
    void aoAtualizar() {
        atualizadoEm = Instant.now();
    }
}