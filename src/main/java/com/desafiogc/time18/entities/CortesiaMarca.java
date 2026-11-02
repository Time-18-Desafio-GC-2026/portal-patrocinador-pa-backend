package com.desafiogc.time18.entities;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "cortesia_marca")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CortesiaMarca {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "marca_id", nullable = false, unique = true)
    private Marca marca;

    @Column(name = "arena_passaporte", nullable = false)
    private int arenaPassaporte;

    @Column(name = "arena_sexta", nullable = false)
    private int arenaSexta;

    @Column(name = "arena_sabado", nullable = false)
    private int arenaSabado;

    @Column(name = "camarote_passaporte", nullable = false)
    private int camarotePassaporte;

    @Column(name = "camarote_sexta", nullable = false)
    private int camaroteSexta;

    @Column(name = "camarote_sabado", nullable = false)
    private int camaroteSabado;

    @Column(name = "premium_passaporte", nullable = false)
    private int premiumPassaporte;

    @Column(name = "premium_sexta", nullable = false)
    private int premiumSexta;

    @Column(name = "premium_sabado", nullable = false)
    private int premiumSabado;

    @Column(name = "lounge_info", nullable = false, columnDefinition = "TEXT")
    private String loungeInfo;

    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;

    @PrePersist
    @PreUpdate
    void aoSalvar() {
        atualizadoEm = Instant.now();
        if (loungeInfo == null) {
            loungeInfo = "";
        }
    }
}