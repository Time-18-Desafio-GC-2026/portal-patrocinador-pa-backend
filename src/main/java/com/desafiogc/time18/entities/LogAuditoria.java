package com.desafiogc.time18.entities;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "log_auditoria")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LogAuditoria {

    @Id
    @Column(length = 7)
    private String id;

    @Column(name = "ator_id", nullable = false)
    private UUID atorId;

    @Column(name = "ator_nome", nullable = false, length = 254)
    private String atorNome;

    @Enumerated(EnumType.STRING)
    @Column(name = "ator_perfil", nullable = false, length = 20)
    private Perfil atorPerfil;

    @Column(name = "ator_marca_nome", length = 150)
    private String atorMarcaNome;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private AcaoAuditoria acao;

    @Column(name = "marca_id")
    private UUID marcaId;

    @Column(name = "marca_nome", length = 150)
    private String marcaNome;

    @Column(length = 255)
    private String detalhe;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm;
}