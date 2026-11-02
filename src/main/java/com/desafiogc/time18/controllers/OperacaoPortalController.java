package com.desafiogc.time18.controllers;

import com.desafiogc.time18.dtos.OperacaoPortalDtos.*;
import com.desafiogc.time18.security.UsuarioAutenticado;
import com.desafiogc.time18.services.OperacaoPortalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class OperacaoPortalController {

    private final OperacaoPortalService operacaoService;

    @PostMapping(value = "/marcas/{marcaId}/materiais", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Tag(name = "6. Arquivos", description = "Envio, consulta de arquivos/materiais, status, autor e data de atualização")
    @Operation(summary = "Registra um novo material (Plano Comercial, MIV/Logo, Planilha de Mídia, Contrato)")
    public MaterialResponse registrarMaterial(@PathVariable UUID marcaId,
                                              @RequestPart("dados") @Valid RegistrarMaterialRequest request,
                                              @RequestPart(value = "arquivo", required = false) MultipartFile arquivo,
                                              @AuthenticationPrincipal UsuarioAutenticado logado) {
        return operacaoService.registrarMaterial(marcaId, request, arquivo, logado);
    }

    @GetMapping("/marcas/{marcaId}/materiais")
    @Tag(name = "6. Arquivos")
    @Operation(summary = "Lista os materiais/arquivos da marca (com autor, status e data de atualização)")
    public List<MaterialResponse> listarMateriais(@PathVariable UUID marcaId,
                                                  @RequestParam(required = false) String abaDestino,
                                                  @AuthenticationPrincipal UsuarioAutenticado logado) {
        return operacaoService.listarMateriais(marcaId, abaDestino, logado);
    }

    @PostMapping("/materiais/{materialId}/acessar")
    @Tag(name = "6. Arquivos")
    @Operation(summary = "Registra download/visualização de um arquivo no histórico de auditoria")
    public MaterialResponse acessarMaterial(@PathVariable UUID materialId,
                                            @AuthenticationPrincipal UsuarioAutenticado logado) {
        return operacaoService.registrarDownloadOuAcesso(materialId, logado);
    }

    @PatchMapping("/materiais/{materialId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Tag(name = "6. Arquivos")
    @Operation(summary = "Atualiza o status de acompanhamento de um arquivo")
    public MaterialResponse atualizarStatus(@PathVariable UUID materialId,
                                            @Valid @RequestBody AtualizarStatusMaterialRequest request,
                                            @AuthenticationPrincipal UsuarioAutenticado logado) {
        return operacaoService.atualizarStatusMaterial(materialId, request.status(), logado);
    }

    @PostMapping("/marcas/{marcaId}/pesquisa-perguntas")
    @Tag(name = "7. Pesquisa e Relatórios", description = "Cadastro das 3 perguntas da pesquisa e extração automática do Relatório")
    @Operation(summary = "Salva uma das 3 perguntas personalizadas da Pesquisa de Ativação")
    public PerguntaPesquisaResponse salvarPergunta(@PathVariable UUID marcaId,
                                                   @Valid @RequestBody SalvarPerguntaPesquisaRequest request,
                                                   @AuthenticationPrincipal UsuarioAutenticado logado) {
        return operacaoService.salvarPerguntaPesquisa(marcaId, request, logado);
    }

    @PostMapping("/marcas/{marcaId}/indicadores/importar-planilha-padrao")
    @PreAuthorize("hasRole('ADMIN')")
    @Tag(name = "7. Pesquisa e Relatórios")
    @Operation(summary = "Consolida os indicadores dos arquivos no banco")
    public List<IndicadorResponse> importarPlanilhaPadrao(@PathVariable UUID marcaId,
                                                          @AuthenticationPrincipal UsuarioAutenticado logado) {
        return operacaoService.importarAmostraPadraoPlanilha(marcaId, logado);
    }

    @GetMapping("/marcas/{marcaId}/relatorio-consolidado")
    @Tag(name = "7. Pesquisa e Relatórios")
    @Operation(summary = "Gera o relatório do patrocinador a partir da base de dados")
    public RelatorioConsolidadoResponse relatorioConsolidado(@PathVariable UUID marcaId,
                                                             @AuthenticationPrincipal UsuarioAutenticado logado) {
        return operacaoService.consultarRelatorioConsolidado(marcaId, logado);
    }
}