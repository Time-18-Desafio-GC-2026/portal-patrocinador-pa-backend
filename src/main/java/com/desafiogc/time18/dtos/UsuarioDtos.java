package com.desafiogc.time18.dtos;

import com.desafiogc.time18.entities.Cota;
import com.desafiogc.time18.entities.Perfil;
import com.desafiogc.time18.entities.Usuario;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.time.Instant;
import java.util.UUID;

/**
 * DTOs para cadastro de e-mails liberados (Allow-list) de Patrocinadores e Admins RBS.
 */
public final class UsuarioDtos {

    // Admin RBS libera um novo e-mail de patrocinador para uma marca (usa a senha padrão da marca)
    public record CriarPatrocinadorRequest(
            @NotBlank(message = "Informe o e-mail do patrocinador/agência.")
            @Email(message = "E-mail inválido.")
            String email
    ) {}

    // Superadmin RBS cria um novo membro do time B2B (Admin RBS)
    public record CriarAdminRequest(
            @NotBlank(message = "Informe o e-mail do administrador RBS.")
            @Email(message = "E-mail inválido.")
            String email,

            String nome,

            @NotBlank(message = "Informe a senha inicial.")
            String senhaInicial
    ) {}

    public record AtualizarUsuarioRequest(
            @Email(message = "E-mail inválido.")
            String email,
            String nome
    ) {}

    public record ResetarSenhaRequest(
            String novaSenhaTemporaria
    ) {}

    // Dados seguros do usuário devolvidos no JSON (inclui nome e cota da marca para ajudar o Front-end a ocultar blocos!)
    public record UsuarioResponse(
            UUID id,
            String email,
            String nome,
            Perfil perfil,
            UUID marcaId,
            String marcaNome,
            Cota marcaCota,
            boolean ativo,
            boolean precisaTrocarSenha,
            Instant primeiroAcessoEm,
            Instant criadoEm
    ) {
        public static UsuarioResponse de(Usuario u) {
            return new UsuarioResponse(
                    u.getId(),
                    u.getEmail(),
                    u.getNome(),
                    u.getPerfil(),
                    u.getMarca() != null ? u.getMarca().getId() : null,
                    u.getMarca() != null ? u.getMarca().getNome() : null,
                    u.getMarca() != null ? u.getMarca().getCota() : null,
                    u.isAtivo(),
                    u.isPrecisaTrocarSenha(),
                    u.getPrimeiroAcessoEm(),
                    u.getCriadoEm()
            );
        }
    }
}