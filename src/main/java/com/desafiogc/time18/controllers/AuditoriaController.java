package com.desafiogc.time18.controllers;

import com.desafiogc.time18.dtos.AuditoriaDtos.LogAuditoriaResponse;
import com.desafiogc.time18.security.UsuarioAutenticado;
import com.desafiogc.time18.services.AuditoriaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/auditoria")
@RequiredArgsConstructor
@Tag(name = "4. Auditoria", description = "Histórico imutável de ações, logins, uploads, downloads e alterações")
public class AuditoriaController {

    private final AuditoriaService auditoriaService;

    @GetMapping
    @Operation(summary = "Lista o histórico de ações realizadas na plataforma, permitindo aos ADMs RBS acompanhar a atividade dos usuários no portal.")
    public List<LogAuditoriaResponse> listar(@RequestParam(required = false) UUID marcaId,
                                             @AuthenticationPrincipal UsuarioAutenticado logado) {
        return auditoriaService.listarHistorico(logado, marcaId);
    }
}