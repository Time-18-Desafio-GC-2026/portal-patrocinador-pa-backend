package com.desafiogc.time18.controllers;

import com.desafiogc.time18.dtos.AutenticacaoDtos.*;
import com.desafiogc.time18.dtos.UsuarioDtos.UsuarioResponse;
import com.desafiogc.time18.security.UsuarioAutenticado;
import com.desafiogc.time18.services.AutenticacaoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "1. Autenticação e Sessão", description = "Login, primeiro acesso e dados do usuário logado")
public class AutenticacaoController {

    private final AutenticacaoService autenticacaoService;

    @PostMapping("/login")
    @Operation(summary = "Realiza login e devolve o Token JWT de autenticação do usuário")
    public SessaoResponse login(@Valid @RequestBody LoginRequest request) {
        return autenticacaoService.login(request);
    }

    @GetMapping("/eu")
    @Operation(summary = "Retorna os dados do usuário logado")
    public UsuarioResponse meusDados(@AuthenticationPrincipal UsuarioAutenticado logado) {
        return autenticacaoService.meusDados(logado);
    }

    @PostMapping("/primeiro-acesso")
    @Operation(summary = "Patrocinador define seu nome e troca a senha padrão da marca")
    public SessaoResponse primeiroAcesso(@AuthenticationPrincipal UsuarioAutenticado logado,
                                         @Valid @RequestBody PrimeiroAcessoRequest request) {
        return autenticacaoService.concluirPrimeiroAcesso(logado, request);
    }

    @PostMapping("/alterar-senha")
    @Operation(summary = "Usuário logado altera sua senha atual")
    public SessaoResponse alterarSenha(@AuthenticationPrincipal UsuarioAutenticado logado,
                                       @Valid @RequestBody AlterarSenhaRequest request) {
        return autenticacaoService.alterarSenha(logado, request);
    }

    @PatchMapping("/eu/nome")
    @Operation(summary = "Atualiza o nome de exibição do usuário logado")
    public UsuarioResponse atualizarNome(@AuthenticationPrincipal UsuarioAutenticado logado,
                                         @Valid @RequestBody AtualizarNomeRequest request) {
        return autenticacaoService.atualizarNome(logado, request);
    }
}