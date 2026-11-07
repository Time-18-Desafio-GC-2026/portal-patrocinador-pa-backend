package com.desafiogc.time18.security;

import com.desafiogc.time18.entities.Perfil;
import java.util.UUID;


public record UsuarioAutenticado(
        UUID id,
        String email,
        String nome,
        Perfil perfil,
        UUID marcaId,
        boolean precisaTrocarSenha
) {}