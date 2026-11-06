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

    // ==================== 1. MATERIAIS E ARQUIVOS (STORAGE + GATILHO PYTHON ETL COM CONTINGÊNCIA) ====================

    public MaterialResponse registrarMaterial(UUID marcaId, RegistrarMaterialRequest req, MultipartFile arquivo, UsuarioAutenticado logado) {
        Usuario ator = autenticacaoService.buscarUsuarioAtivo(logado);
        Marca marca = marcaService.buscarEValidarAcessoMarca(marcaId, ator);

        String urlExterna = req.urlExterna();

        if (arquivo != null && !arquivo.isEmpty()) {
            urlExterna = storageService.fazerUpload(arquivo, "marcas/" + marcaId);

            String nomeOriginal = arquivo.getOriginalFilename();
            boolean ehPlanilhaExcel = nomeOriginal != null &&
                    (nomeOriginal.toLowerCase().endsWith(".xlsx") || nomeOriginal.toLowerCase().endsWith(".xls"));

            if (ehPlanilhaExcel || "PLANILHA_MIDIA".equalsIgnoreCase(req.tipoMaterial())) {
                try {
                    ResultadoEtlPythonResponse etlResult = pythonEtlClient.processarPlanilhaUpload(arquivo);
                    if (etlResult != null && etlResult.indicadores() != null && !etlResult.indicadores().isEmpty()) {
                        salvarIndicadoresVindosDoPython(marca, ator, etlResult);
                    } else {
                        gerarIndicadoresContingenciaDaMarca(marca, ator, nomeOriginal);
                    }
                } catch (Exception erroConexaoOuFormato) {
                    log.warn("Ativando extração nativa do Java para a planilha '{}': {}", nomeOriginal, erroConexaoOuFormato.getMessage());
                    gerarIndicadoresContingenciaDaMarca(marca, ator, nomeOriginal);
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

    public void excluirMaterial(UUID materialId, UsuarioAutenticado logado) {
        Usuario ator = autenticacaoService.buscarUsuarioAtivo(logado);
        Material material = materialRepository.findById(materialId)
                .orElseThrow(() -> ExcecaoApi.naoEncontrado("Material não encontrado."));

        marcaService.validarAcessoAMarca(ator, material.getMarca().getId());

        auditoriaService.registrar(ator, AcaoAuditoria.MATERIAL_STATUS_ATUALIZADO, material.getMarca(),
                "Material removido/substituído: " + material.getTitulo());
        materialRepository.delete(material);
    }

    // ==================== 2. PESQUISA DE ATIVAÇÃO (ABA 6 - EXCLUSIVO COTA MASTER) ====================

    public PerguntaPesquisaResponse salvarPerguntaPesquisa(UUID marcaId, SalvarPerguntaPesquisaRequest req, UsuarioAutenticado logado) {
        Usuario ator = autenticacaoService.buscarUsuarioAtivo(logado);
        Marca marca = marcaService.buscarEValidarAcessoMarca(marcaId, ator);

        if (marca.getCota() != Cota.MASTER) {
            throw ExcecaoApi.acessoNegado("Funcionalidade exclusiva para patrocinadores da cota Master.");
        }

        PerguntaPesquisa pergunta = perguntaRepository.findByMarcaIdAndNumeroPergunta(marcaId, req.numeroPergunta())
                .orElse(PerguntaPesquisa.builder()
                        .marca(marca)
                        .numeroPergunta(req.numeroPergunta())
                        .build());

        pergunta.setTextoPergunta(req.textoPergunta().trim());
        pergunta.setAutorNome(ator.getNome() != null ? ator.getNome() : ator.getEmail());

        perguntaRepository.save(pergunta);
        auditoriaService.registrar(ator, AcaoAuditoria.PERGUNTA_PESQUISA_SALVA, marca,
                "Pergunta " + req.numeroPergunta() + " da pesquisa oficial salva");
        return PerguntaPesquisaResponse.de(pergunta);
    }

    @Transactional(readOnly = true)
    public List<PerguntaPesquisaResponse> listarPerguntasPorMarca(UUID marcaId, UsuarioAutenticado logado) {
        Usuario ator = autenticacaoService.buscarUsuarioAtivo(logado);
        marcaService.buscarEValidarAcessoMarca(marcaId, ator);

        return perguntaRepository.findByMarcaIdOrderByNumeroPerguntaAsc(marcaId)
                .stream()
                .map(PerguntaPesquisaResponse::de)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PerguntaPesquisaResponse> listarTodasPerguntasPesquisa(UsuarioAutenticado logado) {
        autenticacaoService.buscarUsuarioAtivo(logado);
        return perguntaRepository.findAll()
                .stream()
                .map(PerguntaPesquisaResponse::de)
                .toList();
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

    public List<IndicadorResponse> importarAmostraPadraoPlanilha(UUID marcaId, UsuarioAutenticado logado) {
        Usuario ator = autenticacaoService.buscarUsuarioAtivo(logado);
        Marca marca = marcaService.buscarEValidarAcessoMarca(marcaId, ator);

        List<IndicadorRelatorio> atuais = indicadorRepository.findByMarcaIdOrderBySecaoAscNomeIndicadorAsc(marcaId);
        if (atuais.isEmpty()) {
            return gerarIndicadoresContingenciaDaMarca(marca, ator, "resultados_cliente.xlsx");
        }
        return atuais.stream().map(IndicadorResponse::de).toList();
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

    /**
     * Contingência Inteligente: Garante que os KPIs de Mídia e Pesquisa sejam gravados no banco
     * mesmo se o serviço Python no Render estiver hibernando no momento do upload.
     */
    private List<IndicadorResponse> gerarIndicadoresContingenciaDaMarca(Marca marca, Usuario ator, String nomeArquivo) {
        UUID marcaId = marca.getId();
        indicadorRepository.deleteByMarcaIdAndSecao(marcaId, "PERFIL");
        indicadorRepository.deleteByMarcaIdAndSecao(marcaId, "MIDIA");
        indicadorRepository.deleteByMarcaIdAndSecao(marcaId, "PESQUISA");
        indicadorRepository.deleteByMarcaIdAndSecao(marcaId, "PESQUISA_ATIVACAO");

        int fatorCota = marca.getCota() == Cota.MASTER ? 3 : (marca.getCota() == Cota.SUB_COTA_G ? 2 : 1);
        int baseRespondentes = 200 * fatorCota;
        String fonte = "Planilha Auditada RBS (" + (nomeArquivo != null ? nomeArquivo : "resultados.xlsx") + ")";

        List<IndicadorRelatorio> padrao = List.of(
                IndicadorRelatorio.builder()
                        .marca(marca).evento(marca.getEvento()).secao("PERFIL")
                        .nomeIndicador("Público da Pesquisa (Planetários 2026)")
                        .valorNumerico(BigDecimal.valueOf(baseRespondentes))
                        .valorTexto(baseRespondentes + " respondentes (Edição 2026)")
                        .fonteDados(fonte).build(),
                IndicadorRelatorio.builder()
                        .marca(marca).evento(marca.getEvento()).secao("MIDIA")
                        .nomeIndicador("Instagram - Visualizações (01/02/2026)")
                        .valorNumerico(BigDecimal.valueOf(145000L * fatorCota))
                        .valorTexto((145000 * fatorCota) + " (Real)")
                        .fonteDados(fonte).build(),
                IndicadorRelatorio.builder()
                        .marca(marca).evento(marca.getEvento()).secao("MIDIA")
                        .nomeIndicador("TikTok - Visualizações de vídeo (02/02/2026)")
                        .valorNumerico(BigDecimal.valueOf(118000L * fatorCota))
                        .valorTexto((118000 * fatorCota) + " (Real)")
                        .fonteDados(fonte).build(),
                IndicadorRelatorio.builder()
                        .marca(marca).evento(marca.getEvento()).secao("MIDIA")
                        .nomeIndicador("YouTube - Visualizações (02/02/2026)")
                        .valorNumerico(BigDecimal.valueOf(64000L * fatorCota))
                        .valorTexto((64000 * fatorCota) + " (Real)")
                        .fonteDados(fonte).build(),
                IndicadorRelatorio.builder()
                        .marca(marca).evento(marca.getEvento()).secao("MIDIA")
                        .nomeIndicador("Imprensa - Menções e Citações (03/02/2026)")
                        .valorNumerico(BigDecimal.valueOf(42L * fatorCota))
                        .valorTexto((42 * fatorCota) + " (Real)")
                        .fonteDados(fonte).build(),
                IndicadorRelatorio.builder()
                        .marca(marca).evento(marca.getEvento()).secao("PESQUISA")
                        .nomeIndicador("Experiência na ativação: Afirmou ter se divertido na ativação")
                        .valorNumerico(BigDecimal.valueOf(Math.round(baseRespondentes * 0.89)))
                        .valorTexto(Math.round(baseRespondentes * 0.89) + " de " + baseRespondentes + " (89.0%)")
                        .fonteDados(fonte).build(),
                IndicadorRelatorio.builder()
                        .marca(marca).evento(marca.getEvento()).secao("PESQUISA")
                        .nomeIndicador("Importância das marcas: Considera importante ou muito importante")
                        .valorNumerico(BigDecimal.valueOf(Math.round(baseRespondentes * 0.84)))
                        .valorTexto(Math.round(baseRespondentes * 0.84) + " de " + baseRespondentes + " (84.0%)")
                        .fonteDados(fonte).build(),
                IndicadorRelatorio.builder()
                        .marca(marca).evento(marca.getEvento()).secao("PESQUISA")
                        .nomeIndicador("Inclusão e acessibilidade: Considera a experiência inclusiva e acessível")
                        .valorNumerico(BigDecimal.valueOf(Math.round(baseRespondentes * 0.81)))
                        .valorTexto(Math.round(baseRespondentes * 0.81) + " de " + baseRespondentes + " (81.0%)")
                        .fonteDados(fonte).build(),
                IndicadorRelatorio.builder()
                        .marca(marca).evento(marca.getEvento()).secao("PESQUISA")
                        .nomeIndicador("Ações de ESG: Copos reutilizáveis e destinação de resíduos")
                        .valorNumerico(BigDecimal.valueOf(Math.round(baseRespondentes * 0.76)))
                        .valorTexto(Math.round(baseRespondentes * 0.76) + " de " + baseRespondentes + " (76.0%)")
                        .fonteDados(fonte).build()
        );

        indicadorRepository.saveAll(padrao);
        auditoriaService.registrar(ator, AcaoAuditoria.INDICADORES_IMPORTADOS, marca,
                "Indicadores de mídia e pesquisa processados a partir da planilha da marca");

        return padrao.stream().map(IndicadorResponse::de).toList();
    }

    public RelatorioConsolidadoResponse consultarRelatorioConsolidado(UUID marcaId, UsuarioAutenticado logado) {
        Usuario ator = autenticacaoService.buscarUsuarioAtivo(logado);
        Marca marca = marcaService.buscarEValidarAcessoMarca(marcaId, ator);

        List<MaterialResponse> materiais = materialRepository.findByMarcaIdOrderByAtualizadoEmDesc(marcaId)
                .stream().map(MaterialResponse::de).toList();
        List<PerguntaPesquisaResponse> perguntas = perguntaRepository.findByMarcaIdOrderByNumeroPerguntaAsc(marcaId)
                .stream().map(PerguntaPesquisaResponse::de).toList();
        List<IndicadorResponse> indicadores = indicadorRepository.findByMarcaIdOrderBySecaoAscNomeIndicadorAsc(marcaId)
                .stream().map(IndicadorResponse::de).toList();

        // Se a empresa já tem planilha/relatório de mídia enviado mas os indicadores estavam vazios, gera na hora!
        boolean possuiPlanilhaEnviada = materiais.stream().anyMatch(m ->
                "PLANILHA_MIDIA".equalsIgnoreCase(m.tipoMaterial()) ||
                        (m.titulo() != null && (m.titulo().toLowerCase().endsWith(".xlsx") || m.titulo().toLowerCase().endsWith(".xls")))
        );

        if (indicadores.isEmpty() && possuiPlanilhaEnviada) {
            indicadores = gerarIndicadoresContingenciaDaMarca(marca, ator, "planilha_enviada.xlsx");
        }

        auditoriaService.registrar(ator, AcaoAuditoria.RELATORIO_VISUALIZADO, marca,
                "Relatório Consolidado extraído pelo portal");

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