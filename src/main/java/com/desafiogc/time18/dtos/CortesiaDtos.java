package com.desafiogc.time18.dtos;

import com.desafiogc.time18.entities.CortesiaMarca;
import jakarta.validation.constraints.Min;

import java.time.Instant;
import java.util.UUID;

public final class CortesiaDtos {

    private CortesiaDtos() {}

    public record SalvarCortesiaRequest(
            @Min(0) int arenaPassaporte,
            @Min(0) int arenaSexta,
            @Min(0) int arenaSabado,
            @Min(0) int camarotePassaporte,
            @Min(0) int camaroteSexta,
            @Min(0) int camaroteSabado,
            @Min(0) int premiumPassaporte,
            @Min(0) int premiumSexta,
            @Min(0) int premiumSabado,
            String loungeInfo
    ) {}

    public record CortesiaResponse(
            UUID marcaId,
            int arenaPassaporte,
            int arenaSexta,
            int arenaSabado,
            int camarotePassaporte,
            int camaroteSexta,
            int camaroteSabado,
            int premiumPassaporte,
            int premiumSexta,
            int premiumSabado,
            String loungeInfo,
            Instant atualizadoEm
    ) {
        public static CortesiaResponse de(CortesiaMarca c) {
            return new CortesiaResponse(
                    c.getMarca().getId(),
                    c.getArenaPassaporte(),
                    c.getArenaSexta(),
                    c.getArenaSabado(),
                    c.getCamarotePassaporte(),
                    c.getCamaroteSexta(),
                    c.getCamaroteSabado(),
                    c.getPremiumPassaporte(),
                    c.getPremiumSexta(),
                    c.getPremiumSabado(),
                    c.getLoungeInfo() != null ? c.getLoungeInfo() : "",
                    c.getAtualizadoEm()
            );
        }

        public static CortesiaResponse zerado(UUID marcaId) {
            return new CortesiaResponse(marcaId, 0, 0, 0, 0, 0, 0, 0, 0, 0, "", null);
        }
    }
}