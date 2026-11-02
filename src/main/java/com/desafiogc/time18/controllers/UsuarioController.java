package com.desafiogc.time18.controllers;

import com.desafiogc.time18.dtos.UsuarioDtos.*;
import com.desafiogc.time18.security.UsuarioAutenticado;
import com.desafiogc.time18.services.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Tag(name = "3. Usuários", description = "Liberação de e-mails de patrocinadores e gestão de administradores RBS")
public class UsuarioController {

    private final UsuarioService usuarioService;

    @PostMapping("/marcas/{marcaId}/patrocinadores")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Libera um novo e-mail de patrocinador para uma marca")
    public UsuarioResponse criarPatrocinador(@PathVariable UUID marcaId,
                                             @Valid @RequestBody CriarPatrocinadorRequest request,
                                             @AuthenticationPrincipal UsuarioAutenticado logado) {
        return usuarioService.criarPatrocinador(marcaId, request, logado);
    }

    @PostMapping("/admins")
    @PreAuthorize("hasRole('SUPERADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cadastra um novo membro do time B2B como Admin RBS")
    public UsuarioResponse criarAdmin(@Valid @RequestBody CriarAdminRequest request,
                                      @AuthenticationPrincipal UsuarioAutenticado logado) {
        return usuarioService.criarAdmin(request, logado);
    }

    @GetMapping("/usuarios")
    @Operation(summary = "Lista usuários (Admin vê todos / Patrocinador vê apenas colegas da própria marca)")
    public List<UsuarioResponse> listar(@RequestParam(required = false) UUID marcaId,
                                        @AuthenticationPrincipal UsuarioAutenticado logado) {
        return usuarioService.listar(marcaId, logado);
    }

    @PostMapping("/usuarios/{id}/desativar")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Desativa temporariamente o acesso de um usuário")
    public UsuarioResponse desativar(@PathVariable UUID id,
                                     @AuthenticationPrincipal UsuarioAutenticado logado) {
        return usuarioService.desativar(id, logado);
    }

    @PostMapping("/usuarios/{id}/reativar")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Reativa o acesso de um usuário")
    public UsuarioResponse reativar(@PathVariable UUID id,
                                    @AuthenticationPrincipal UsuarioAutenticado logado) {
        return usuarioService.reativar(id, logado);
    }

    @PostMapping("/usuarios/{id}/resetar-senha")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Reseta a senha do patrocinador para a senha padrão da marca")
    public UsuarioResponse resetarSenha(@PathVariable UUID id,
                                        @RequestBody(required = false) ResetarSenhaRequest request,
                                        @AuthenticationPrincipal UsuarioAutenticado logado) {
        return usuarioService.resetarSenha(id, request != null ? request.novaSenhaTemporaria() : null, logado);
    }

    @DeleteMapping("/usuarios/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Exclui definitivamente um usuário desativado")
    public void excluir(@PathVariable UUID id,
                        @AuthenticationPrincipal UsuarioAutenticado logado) {
        usuarioService.excluir(id, logado);
    }
}