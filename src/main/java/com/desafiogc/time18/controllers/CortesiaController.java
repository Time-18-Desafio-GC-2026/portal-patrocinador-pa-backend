package com.desafiogc.time18.controllers;

import com.desafiogc.time18.dtos.CortesiaDtos.CortesiaResponse;
import com.desafiogc.time18.dtos.CortesiaDtos.SalvarCortesiaRequest;
import com.desafiogc.time18.security.UsuarioAutenticado;
import com.desafiogc.time18.services.CortesiaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/marcas/{marcaId}/cortesias")
@RequiredArgsConstructor
@Tag(name = "2. Marcas e Cotas", description = "Cadastro de patrocinadores, cotas (Master, Subcota G, Subcota) e isolamento por marca")
public class CortesiaController {

    private final CortesiaService cortesiaService;

    @GetMapping
    @Operation(summary = "Consulta o quadro de cortesias de uma marca (Arena, Camarote, Premium e Lounge)")
    public CortesiaResponse buscar(@PathVariable UUID marcaId,
                                   @AuthenticationPrincipal UsuarioAutenticado logado) {
        return cortesiaService.buscarPorMarca(marcaId, logado);
    }

    @PutMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Atualiza as quantidades de cortesias e informações de Lounge da marca")
    public CortesiaResponse salvar(@PathVariable UUID marcaId,
                                   @Valid @RequestBody SalvarCortesiaRequest request,
                                   @AuthenticationPrincipal UsuarioAutenticado logado) {
        return cortesiaService.salvar(marcaId, request, logado);
    }
}