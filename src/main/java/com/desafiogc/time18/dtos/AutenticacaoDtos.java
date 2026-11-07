package com.desafiogc.time18.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;


public final class AutenticacaoDtos {

    
    public record LoginRequest(
            @NotBlank(message = "Informe o e-mail.")
            @Email(message = "Formato de e-mail inválido.")
            String email,

            @NotBlank(message = "Informe a senha.")
            String senha
    ) {}

    
    public record SessaoResponse(
            String tokenJwt,
            String tipoToken,
            UsuarioDtos.UsuarioResponse usuario
    ) {}

    
    public record PrimeiroAcessoRequest(
            @NotBlank(message = "Informe o seu nome para identificação no portal.")
            String nome,

            @NotBlank(message = "Informe a sua nova senha pessoal (mínimo 8 caracteres).")
            String novaSenha
    ) {}

    
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