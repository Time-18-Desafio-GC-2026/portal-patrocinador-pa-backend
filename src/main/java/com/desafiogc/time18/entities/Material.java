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

    // A qual marca patrocinadora este arquivo/material pertence
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "marca_id", nullable = false)
    private Marca marca;

    @Column(nullable = false, length = 150)
    private String titulo;

    @Column(length = 500)
    private String descricao;

    // Define em qual das 7 abas do portal este material aparece
    @Column(name = "aba_destino", nullable = false, length = 30)
    private String abaDestino;

    // Ex: PLANO_COMERCIAL, MIV_LOGO, PLANILHA_MIDIA, LINK_POST, CONTRATO
    @Column(name = "tipo_material", nullable = false, length = 30)
    private String tipoMaterial;

    // Caminho interno no bucket "arquivos-portal-patrocinador-pa" do Supabase Storage
    @Column(name = "caminho_storage", length = 500)
    private String caminhoStorage;

    // Usado quando a RBS ou patrocinador cadastra um link (ex: post do Instagram na Aba 3)
    @Column(name = "url_externa", length = 500)
    private String urlExterna;

    // Status de acompanhamento: ENVIADO, EM_ANALISE, APROVADO, PROCESSADO
    @Column(nullable = false, length = 30)
    private String status;

    // Campos obrigatórios pelo Critério de Aceite da Entrega 2 (Autor e Data de atualização)
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