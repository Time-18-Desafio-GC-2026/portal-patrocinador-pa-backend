package com.desafiogc.time18.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Agrupa todos os DTOs (records) relacionados a Login e Senha.
 * Usamos "record" do Java 21 porque são imutáveis e o Jackson converte para JSON automaticamente.
 */
public final class AutenticacaoDtos {

    // O que o Front-end envia em POST /auth/login
    public record LoginRequest(
            @NotBlank(message = "Informe o e-mail.")
            @Email(message = "Formato de e-mail inválido.")
            String email,

            @NotBlank(message = "Informe a senha.")
            String senha
    ) {}

    // O que a API devolve quando o login dá certo (Token JWT + Dados do Usuário logado)
    public record SessaoResponse(
            String tokenJwt,
            String tipoToken,
            UsuarioDtos.UsuarioResponse usuario
    ) {}

    // O que o patrocinador envia no primeiro acesso para definir seu nome e trocar a senha padrão da marca
    public record PrimeiroAcessoRequest(
            @NotBlank(message = "Informe o seu nome para identificação no portal.")
            String nome,

            @NotBlank(message = "Informe a sua nova senha pessoal (mínimo 8 caracteres).")
            String novaSenha
    ) {}

    // O que o usuário logado envia quando quer trocar a própria senha depois
    public record AlterarSenhaRequest(
            @NotBlank(message = "Informe a senha atual.")
            String senhaAtual,

            @NotBlank(message = "Informe a nova senha.")
            String novaSenha
    ) {}

    public record AtualizarNomeRequest(
            @NotBlank(message = "Informe o nome de usuário.")
            String nome
    ) {}
}