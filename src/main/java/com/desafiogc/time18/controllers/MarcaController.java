package com.desafiogc.time18.controllers;

import com.desafiogc.time18.dtos.MarcaDtos.*;
import com.desafiogc.time18.security.UsuarioAutenticado;
import com.desafiogc.time18.services.MarcaService;
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
@RequestMapping("/marcas")
@RequiredArgsConstructor
@Tag(name = "2. Marcas e Cotas", description = "Cadastro de patrocinadores, cotas (Master, Subcota G, Subcota) e isolamento por marca")
public class MarcaController {

    private final MarcaService marcaService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cadastra uma nova marca patrocinadora e sua cota")
    public MarcaResponse criar(@Valid @RequestBody CriarMarcaRequest request,
                               @AuthenticationPrincipal UsuarioAutenticado logado) {
        return marcaService.criar(request, logado);
    }

    @GetMapping
    @Operation(summary = "Lista marcas (Admin RBS vê todas / Patrocinador vê apenas a própria)")
    public List<MarcaResponse> listar(@RequestParam(required = false) Boolean ativa,
                                      @AuthenticationPrincipal UsuarioAutenticado logado) {
        return marcaService.listar(ativa, logado);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca detalhes de uma marca (Patrocinador é impedido de acessar dados de outra marca)")
    public MarcaResponse buscarPorId(@PathVariable UUID id,
                                     @AuthenticationPrincipal UsuarioAutenticado logado) {
        return marcaService.buscarPorId(id, logado);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Atualiza nome, CNPJ ou nível de cota da marca")
    public MarcaResponse atualizar(@PathVariable UUID id,
                                   @Valid @RequestBody AtualizarMarcaRequest request,
                                   @AuthenticationPrincipal UsuarioAutenticado logado) {
        return marcaService.atualizar(id, request, logado);
    }

    @PutMapping("/{id}/senha-padrao")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Altera a senha padrão inicial da marca")
    public MarcaResponse alterarSenhaPadrao(@PathVariable UUID id,
                                            @Valid @RequestBody AlterarSenhaPadraoRequest request,
                                            @AuthenticationPrincipal UsuarioAutenticado logado) {
        return marcaService.alterarSenhaPadrao(id, request.novaSenhaPadrao(), logado);
    }

    @PostMapping("/{id}/desativar")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Desativa uma marca e bloqueia o acesso dos usuários dela")
    public MarcaResponse desativar(@PathVariable UUID id,
                                   @AuthenticationPrincipal UsuarioAutenticado logado) {
        return marcaService.desativar(id, logado);
    }

    @PostMapping("/{id}/reativar")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Reativa uma marca")
    public MarcaResponse reativar(@PathVariable UUID id,
                                  @AuthenticationPrincipal UsuarioAutenticado logado) {
        return marcaService.reativar(id, logado);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Exclui definitivamente uma marca previamente desativada")
    public void excluir(@PathVariable UUID id,
                        @AuthenticationPrincipal UsuarioAutenticado logado) {
        marcaService.excluir(id, logado);
    }
}