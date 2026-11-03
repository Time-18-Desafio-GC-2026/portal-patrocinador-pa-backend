package com.desafiogc.time18.services;

import com.desafiogc.time18.common.ExcecaoApi;
import com.desafiogc.time18.dtos.OperacaoPortalDtos.*;
import com.desafiogc.time18.dtos.PythonEtlDtos.IndicadorEtlDto;
import com.desafiogc.time18.dtos.PythonEtlDtos.ResultadoEtlPythonResponse;
import com.desafiogc.time18.entities.*;
import com.desafiogc.time18.repositories.IndicadorRelatorioRepository;
import com.desafiogc.time18.repositories.MaterialRepository;
import com.desafiogc.time18.repositories.PerguntaPesquisaRepository;
import com.desafiogc.time18.security.UsuarioAutenticado;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class OperacaoPortalService {

    private final MaterialRepository materialRepository;
    private final PerguntaPesquisaRepository perguntaRepository;
    private final IndicadorRelatorioRepository indicadorRepository;
    private final MarcaService marcaService;
    private final AutenticacaoService autenticacaoService;
    private final AuditoriaService auditoriaService;
    private final SupabaseStorageService storageService;
    private final PythonEtlClient pythonEtlClient;

    // ==================== 1. MATERIAIS E ARQUIVOS (STORAGE + GATILHO PYTHON ETL) ====================

    public MaterialResponse registrarMaterial(UUID marcaId, RegistrarMaterialRequest req, MultipartFile arquivo, UsuarioAutenticado logado) {
        Usuario ator = autenticacaoService.buscarUsuarioAtivo(logado);
        Marca marca = marcaService.buscarEValidarAcessoMarca(marcaId, ator);

        String urlExterna = req.urlExterna();

        if (arquivo != null && !arquivo.isEmpty()) {
            urlExterna = storageService.fazerUpload(arquivo, "marcas/" + marcaId);

            // GATILHO PYTHON: Se o arquivo enviado for uma planilha .xlsx, envia para o Python validar e extrair KPIs da marca!
            String nomeOriginal = arquivo.getOriginalFilename();
            if (nomeOriginal != null && nomeOriginal.toLowerCase().endsWith(".xlsx")) {
                try {
                    ResultadoEtlPythonResponse etlResult = pythonEtlClient.processarPlanilhaUpload(arquivo);
                    if (etlResult != null && etlResult.indicadores() != null) {
                        salvarIndicadoresVindosDoPython(marca, ator, etlResult);
                    }
                } catch (IllegalStateException erroConexao) {
                    log.warn("Upload salvo no Storage, mas o serviço Python está offline no momento: {}", erroConexao.getMessage());
                }
            }
        }

        Material material = Material.builder()
                .marca(marca)
                .titulo(req.titulo().trim())
                .descricao(req.descricao())
                .abaDestino(req.abaDestino().trim().toUpperCase())
                .tipoMaterial(req.tipoMaterial().trim().toUpperCase())
                .caminhoStorage(urlExterna)
                .urlExterna(urlExterna)
                .status("ENVIADO")
                .autorId(ator.getId())
                .autorNome(ator.getNome() != null ? ator.getNome() : ator.getEmail())
                .autorPerfil(ator.getPerfil())
                .build();

        materialRepository.save(material);
        auditoriaService.registrar(ator, AcaoAuditoria.MATERIAL_ENVIADO, marca,
                "Material registrado: " + material.getTitulo() + " (" + material.getAbaDestino() + ")");
        return MaterialResponse.de(material);
    }

    @Transactional(readOnly = true)
    public List<MaterialResponse> listarMateriais(UUID marcaId, String abaDestino, UsuarioAutenticado logado) {
        Usuario ator = autenticacaoService.buscarUsuarioAtivo(logado);
        marcaService.validarAcessoAMarca(ator, marcaId);

        List<Material> lista = (abaDestino == null || abaDestino.isBlank())
                ? materialRepository.findByMarcaIdOrderByAtualizadoEmDesc(marcaId)
                : materialRepository.findByMarcaIdAndAbaDestinoOrderByAtualizadoEmDesc(marcaId, abaDestino.trim().toUpperCase());

        return lista.stream().map(MaterialResponse::de).toList();
    }

    public MaterialResponse registrarDownloadOuAcesso(UUID materialId, UsuarioAutenticado logado) {
        Usuario ator = autenticacaoService.buscarUsuarioAtivo(logado);
        Material material = materialRepository.findById(materialId)
                .orElseThrow(() -> ExcecaoApi.naoEncontrado("Material não encontrado."));

        marcaService.validarAcessoAMarca(ator, material.getMarca().getId());

        auditoriaService.registrar(ator, AcaoAuditoria.MATERIAL_BAIXADO, material.getMarca(),
                "Acessou/Baixou o material: " + material.getTitulo());
        return MaterialResponse.de(material);
    }

    public MaterialResponse atualizarStatusMaterial(UUID materialId, String novoStatus, UsuarioAutenticado logado) {
        Usuario admin = autenticacaoService.buscarUsuarioAtivo(logado);
        Material material = materialRepository.findById(materialId)
                .orElseThrow(() -> ExcecaoApi.naoEncontrado("Material não encontrado."));

        material.setStatus(novoStatus.trim().toUpperCase());
        auditoriaService.registrar(admin, AcaoAuditoria.MATERIAL_STATUS_ATUALIZADO, material.getMarca(),
                "Status do material '" + material.getTitulo() + "' alterado para " + material.getStatus());
        return MaterialResponse.de(material);
    }

    // ==================== 2. PESQUISA DE ATIVAÇÃO (ABA 6 - ATÉ 3 PERGUNTAS) ====================

    public PerguntaPesquisaResponse salvarPerguntaPesquisa(UUID marcaId, SalvarPerguntaPesquisaRequest req, UsuarioAutenticado logado) {
        Usuario ator = autenticacaoService.buscarUsuarioAtivo(logado);
        Marca marca = marcaService.buscarEValidarAcessoMarca(marcaId, ator);

        PerguntaPesquisa pergunta = perguntaRepository.findByMarcaIdAndNumeroPergunta(marcaId, req.numeroPergunta())
                .orElse(PerguntaPesquisa.builder()
                        .marca(marca)
                        .numeroPergunta(req.numeroPergunta())
                        .build());

        pergunta.setTextoPergunta(req.textoPergunta().trim());
        pergunta.setAutorNome(ator.getNome() != null ? ator.getNome() : ator.getEmail());

        perguntaRepository.save(pergunta);
        auditoriaService.registrar(ator, AcaoAuditoria.PERGUNTA_PESQUISA_SALVA, marca,
                "Pergunta " + req.numeroPergunta() + " da pesquisa salva");
        return PerguntaPesquisaResponse.de(pergunta);
    }

    // ==================== 3. RELATÓRIO CONSOLIDADO E INTEGRAÇÃO COM PYTHON ====================

    public IndicadorResponse adicionarIndicador(UUID marcaId, SalvarIndicadorRequest req, UsuarioAutenticado logado) {
        Usuario admin = autenticacaoService.buscarUsuarioAtivo(logado);
        Marca marca = marcaService.buscarEValidarAcessoMarca(marcaId, admin);

        IndicadorRelatorio ind = IndicadorRelatorio.builder()
                .marca(marca)
                .evento(marca.getEvento())
                .secao(req.secao().trim().toUpperCase())
                .nomeIndicador(req.nomeIndicador().trim())
                .valorNumerico(req.valorNumerico())
                .valorTexto(req.valorTexto())
                .fonteDados(req.fonteDados().trim())
                .build();

        indicadorRepository.save(ind);
        auditoriaService.registrar(admin, AcaoAuditoria.INDICADORES_IMPORTADOS, marca,
                "Indicador consolidado: " + ind.getNomeIndicador());
        return IndicadorResponse.de(ind);
    }

    @Transactional(readOnly = true)
    public List<IndicadorResponse> importarAmostraPadraoPlanilha(UUID marcaId, UsuarioAutenticado logado) {
        Usuario ator = autenticacaoService.buscarUsuarioAtivo(logado);
        marcaService.buscarEValidarAcessoMarca(marcaId, ator);

        return indicadorRepository.findByMarcaIdOrderBySecaoAscNomeIndicadorAsc(marcaId)
                .stream()
                .map(IndicadorResponse::de)
                .toList();
    }

    private List<IndicadorResponse> salvarIndicadoresVindosDoPython(
            Marca marca,
            Usuario ator,
            ResultadoEtlPythonResponse etlResult
    ) {
        UUID marcaId = marca.getId();
        indicadorRepository.deleteByMarcaIdAndSecao(marcaId, "PERFIL");
        indicadorRepository.deleteByMarcaIdAndSecao(marcaId, "MIDIA");
        indicadorRepository.deleteByMarcaIdAndSecao(marcaId, "PESQUISA");
        indicadorRepository.deleteByMarcaIdAndSecao(marcaId, "PESQUISA_ATIVACAO");

        List<IndicadorRelatorio> entidades = new ArrayList<>();
        for (IndicadorEtlDto dto : etlResult.indicadores()) {
            BigDecimal valorNum = dto.valorNumerico() != null
                    ? BigDecimal.valueOf(dto.valorNumerico())
                    : null;

            entidades.add(IndicadorRelatorio.builder()
                    .marca(marca)
                    .evento(marca.getEvento())
                    .secao(dto.secao() != null ? dto.secao().trim().toUpperCase() : "GERAL")
                    .nomeIndicador(dto.nomeIndicador())
                    .valorNumerico(valorNum)
                    .valorTexto(dto.valorTexto())
                    .fonteDados(dto.fonteDados())
                    .build());
        }

        indicadorRepository.saveAll(entidades);

        auditoriaService.registrar(
                ator,
                AcaoAuditoria.INDICADORES_IMPORTADOS,
                marca,
                String.format(
                        "Pipeline Python ETL concluído: %d indicadores extraídos (%d mídias, %d pesquisas)",
                        entidades.size(),
                        etlResult.totalMidiaProcessada(),
                        etlResult.totalPesquisaProcessada()
                )
        );

        return entidades.stream().map(IndicadorResponse::de).toList();
    }

    @Transactional(readOnly = true)
    public RelatorioConsolidadoResponse consultarRelatorioConsolidado(UUID marcaId, UsuarioAutenticado logado) {
        Usuario ator = autenticacaoService.buscarUsuarioAtivo(logado);
        Marca marca = marcaService.buscarEValidarAcessoMarca(marcaId, ator);

        List<MaterialResponse> materiais = materialRepository.findByMarcaIdOrderByAtualizadoEmDesc(marcaId)
                .stream().map(MaterialResponse::de).toList();
        List<PerguntaPesquisaResponse> perguntas = perguntaRepository.findByMarcaIdOrderByNumeroPerguntaAsc(marcaId)
                .stream().map(PerguntaPesquisaResponse::de).toList();
        List<IndicadorResponse> indicadores = indicadorRepository.findByMarcaIdOrderBySecaoAscNomeIndicadorAsc(marcaId)
                .stream().map(IndicadorResponse::de).toList();

        auditoriaService.registrar(ator, AcaoAuditoria.RELATORIO_VISUALIZADO, marca,
                "Relatório Consolidado V1 extraído pelo portal");

        return new RelatorioConsolidadoResponse(
                marca.getId(),
                marca.getNome(),
                marca.getCota(),
                marca.getEvento(),
                Instant.now(),
                materiais,
                perguntas,
                indicadores
        );
    }
}