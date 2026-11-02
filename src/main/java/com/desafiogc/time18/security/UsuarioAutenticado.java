package com.desafiogc.time18.security;

import com.desafiogc.time18.entities.Perfil;
import java.util.UUID;

/**
 * Representa o "Crachá" do usuário que já foi validado pelo filtro JWT em cada requisição.
 * Carrega o marcaId diretamente do token assinado para garantir o isolamento entre patrocinadores!
 */
public record UsuarioAutenticado(
        UUID id,
        String email,
        String nome,
        Perfil perfil,
        UUID marcaId,
        boolean precisaTrocarSenha
) {}