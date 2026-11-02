package com.desafiogc.time18.dtos;

import com.desafiogc.time18.entities.Cota;
import com.desafiogc.time18.entities.Marca;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

/**
 * DTOs para gestão de Marcas Patrocinadoras e suas Cotas (MASTER, SUB_COTA_G, SUB_COTA).
 */
public final class MarcaDtos {

    public record CriarMarcaRequest(
            @NotBlank(message = "Informe o nome da marca patrocinadora.")
            @Size(max = 150, message = "O nome deve ter no máximo 150 caracteres.")
            String nome,

            @NotBlank(message = "Informe o CNPJ da marca (com ou sem pontuação).")
            String cnpj,

            @NotNull(message = "Informe a cota de patrocínio (MASTER, SUB_COTA_G ou SUB_COTA).")
            Cota cota,

            @NotBlank(message = "Informe a senha inicial padrão para os usuários desta marca.")
            String senhaPadrao
    ) {}

    public record AtualizarMarcaRequest(
            @NotBlank(message = "Informe o nome da marca.")
            @Size(max = 150)
            String nome,

            @NotBlank(message = "Informe o CNPJ.")
            String cnpj,

            @NotNull(message = "Informe a cota (MASTER, SUB_COTA_G ou SUB_COTA).")
            Cota cota
    ) {}

    public record AlterarSenhaPadraoRequest(
            @NotBlank(message = "Informe a nova senha padrão da marca.")
            String novaSenhaPadrao
    ) {}

    // O que a API devolve (nunca expõe o hash da senha padrão!)
    public record MarcaResponse(
            UUID id,
            String nome,
            String cnpj,
            Cota cota,
            String evento,
            boolean ativa,
            Instant criadoEm,
            Instant atualizadoEm
    ) {
        public static MarcaResponse de(Marca m) {
            return new MarcaResponse(
                    m.getId(),
                    m.getNome(),
                    m.getCnpj(),
                    m.getCota(),
                    m.getEvento(),
                    m.isAtiva(),
                    m.getCriadoEm(),
                    m.getAtualizadoEm()
            );
        }
    }
}