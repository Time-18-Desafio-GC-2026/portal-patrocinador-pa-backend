package com.desafiogc.time18.dtos;

import com.desafiogc.time18.entities.AcaoAuditoria;
import com.desafiogc.time18.entities.Perfil;

import java.time.Instant;
import java.util.UUID;

public final class AuditoriaDtos {

    public record LogAuditoriaResponse(
            String id,
            String atorNome,
            Perfil atorPerfil,
            String atorMarcaNome,
            AcaoAuditoria acao,
            UUID marcaId,
            String marcaNome,
            String detalhe,
            Instant criadoEm
    ) {}
}