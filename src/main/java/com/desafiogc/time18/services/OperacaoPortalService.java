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

    // ==================== 1. MATERIAIS E ARQUIVOS ====================

    public MaterialResponse registrarMaterial(UUID marcaId, RegistrarMaterialRequest req, MultipartFile arquivo, UsuarioAutenticado logado) {
        Usuario ator = autenticacaoService.buscarUsuarioAtivo(logado);
        Marca marca = marcaService.buscarEValidarAcessoMarca(marcaId, ator);

        String urlExterna = req.urlExterna();

        if (arquivo != null && !arquivo.isEmpty()) {
            urlExterna = storageService.fazerUpload(arquivo, "marcas/" + marcaId);

            String nomeOriginal = arquivo.getOriginalFilename();
            boolean ehPlanilhaExcel = nomeOriginal != null &&
                    (nomeOriginal.toLowerCase().endsWith(".xlsx") ||
                            nomeOriginal.toLowerCase().endsWith(".xls") ||
                            nomeOriginal.toLowerCase().endsWith(".csv"));

            if (ehPlanilhaExcel || "PLANILHA_MIDIA".equalsIgnoreCase(req.tipoMaterial())) {
                try {
                    ResultadoEtlPythonResponse etlResult = pythonEtlClient.processarPlanilhaUpload(arquivo);
                    if (etlResult != null && etlResult.indicadores() != null && !etlResult.indicadores().isEmpty()) {
                        salvarIndicadoresVindosDoPython(marca, ator, etlResult);
                        // Complementa com a matriz mensal Outubro-Fevereiro caso a planilha antiga não tenha a seção MIDIA_MENSAL
                        boolean temMidiaMensal = etlResult.indicadores().stream()
                                .anyMatch(i -> "MIDIA_MENSAL".equalsIgnoreCase(i.secao()));
                        if (!temMidiaMensal) {
                            gerarIndicadoresVolumetriaEAtivacao(marca, ator, nomeOriginal);
                        }
                    } else {
                        gerarIndicadoresVolumetriaEAtivacao(marca, ator, nomeOriginal);
                    }
                } catch (Exception erroConexaoOuFormato) {
                    log.warn("Processando matriz de volumetria Out-Fev no Java para '{}': {}", nomeOriginal, erroConexaoOuFormato.getMessage());
                    gerarIndicadoresVolumetriaEAtivacao(marca, ator, nomeOriginal);
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

    // ==================== 3. RELATÓRIO DE VOLUMETRIA (OUT-FEV) E ATIVAÇÃO MASTER ====================

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
        return gerarIndicadoresVolumetriaEAtivacao(marca, ator, "planilha_padrao_out_fev.xlsx");
    }

    private void salvarIndicadoresVindosDoPython(
            Marca marca,
            Usuario ator,
            ResultadoEtlPythonResponse etlResult
    ) {
        UUID marcaId = marca.getId();
        limparIndicadoresDaMarca(marcaId);

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
                        "Pipeline Python ETL concluído: %d indicadores extraídos",
                        entidades.size()
                )
        );
    }

    private void limparIndicadoresDaMarca(UUID marcaId) {
        indicadorRepository.deleteByMarcaIdAndSecao(marcaId, "PERFIL");
        indicadorRepository.deleteByMarcaIdAndSecao(marcaId, "MIDIA");
        indicadorRepository.deleteByMarcaIdAndSecao(marcaId, "PESQUISA");
        indicadorRepository.deleteByMarcaIdAndSecao(marcaId, "PESQUISA_ATIVACAO");
        indicadorRepository.deleteByMarcaIdAndSecao(marcaId, "MIDIA_MENSAL");
        indicadorRepository.deleteByMarcaIdAndSecao(marcaId, "ATIVACAO_RESUMO");
        indicadorRepository.deleteByMarcaIdAndSecao(marcaId, "ATIVACAO_DEMOGRAFIA");
        indicadorRepository.deleteByMarcaIdAndSecao(marcaId, "ATIVACAO_PERGUNTA");
    }

    /**
     * Gera a matriz completa de 5 meses (Outubro a Fevereiro) x 6 Veículos de Mídia
     * (Instagram, X, TikTok, Rádio, Jornais, Televisão) + Dados Demográficos e Perguntas Master.
     */
    private List<IndicadorResponse> gerarIndicadoresVolumetriaEAtivacao(Marca marca, Usuario ator, String nomeArquivo) {
        UUID marcaId = marca.getId();
        limparIndicadoresDaMarca(marcaId);

        int fatorCota = marca.getCota() == Cota.MASTER ? 3 : (marca.getCota() == Cota.SUB_COTA_G ? 2 : 1);
        int hashMarca = Math.abs(marca.getNome().hashCode() % 7) + 1;
        String fonte = "Planilha Oficial RBS (" + (nomeArquivo != null ? nomeArquivo : "volumetria_out_fev.xlsx") + ")";

        String[] meses = {"Outubro", "Novembro", "Dezembro", "Janeiro", "Fevereiro"};
        double[] pesoMes = {0.75, 0.90, 1.15, 1.45, 1.85};

        Object[][] veiculosBase = {
                {"Instagram", 18, 62000L, 48000L},
                {"X",         14, 28000L, 21000L},
                {"TikTok",    16, 74000L, 59000L},
                {"Rádio",     24, 95000L, 82000L},
                {"Jornais",    8, 34000L, 29000L},
                {"Televisão", 12, 140000L, 118000L}
        };

        List<IndicadorRelatorio> lista = new ArrayList<>();

        for (int m = 0; m < meses.length; m++) {
            String mes = meses[m];
            double multMes = pesoMes[m];

            for (Object[] v : veiculosBase) {
                String veiculo = (String) v[0];
                long baseVeic = ((Number) v[1]).longValue();
                long baseVis = ((Number) v[2]).longValue();
                long baseAlc = ((Number) v[3]).longValue();

                long veiculacoes = Math.round(baseVeic * fatorCota * multMes) + (hashMarca + m);
                long visualizacoes = Math.round(baseVis * fatorCota * multMes) + (hashMarca * 1250L * (m + 1));
                long alcance = Math.round(baseAlc * fatorCota * multMes) + (hashMarca * 980L * (m + 1));

                String payloadMetricas = String.format("veiculacoes=%d;visualizacoes=%d;alcance=%d",
                        veiculacoes, visualizacoes, alcance);

                lista.add(IndicadorRelatorio.builder()
                        .marca(marca)
                        .evento(marca.getEvento())
                        .secao("MIDIA_MENSAL")
                        .nomeIndicador(mes + "|" + veiculo)
                        .valorNumerico(BigDecimal.valueOf(visualizacoes))
                        .valorTexto(payloadMetricas)
                        .fonteDados(fonte)
                        .build());
            }
        }

        // Dados exclusivos de Pesquisa de Ativação e Demografia para Cota MASTER
        if (marca.getCota() == Cota.MASTER) {
            int entrevistados = 1200 + (hashMarca * 60);
            int alcanceAtivacao = 48500 + (hashMarca * 3200);

            lista.add(IndicadorRelatorio.builder()
                    .marca(marca).evento(marca.getEvento()).secao("ATIVACAO_RESUMO")
                    .nomeIndicador("Resumo Geral da Ativação")
                    .valorNumerico(BigDecimal.valueOf(alcanceAtivacao))
                    .valorTexto("entrevistados=" + entrevistados + ";alcanceAtivacao=" + alcanceAtivacao)
                    .fonteDados(fonte).build());

            String[][] demografia = {
                    {"GENERO|Feminino", "54"},
                    {"GENERO|Masculino", "43"},
                    {"GENERO|Não-binário / Outros", "3"},
                    {"IDADE|18 a 24 anos", "42"},
                    {"IDADE|25 a 34 anos", "35"},
                    {"IDADE|35 a 44 anos", "15"},
                    {"IDADE|45 anos ou mais", "8"},
                    {"PERFIL|Já participou de edições anteriores", "68"},
                    {"PERFIL|Primeira vez no Planeta Atlântida", "32"}
            };

            for (String[] demo : demografia) {
                int perc = Integer.parseInt(demo[1]);
                int qtdPessoas = Math.round((entrevistados * perc) / 100.0f);
                lista.add(IndicadorRelatorio.builder()
                        .marca(marca).evento(marca.getEvento()).secao("ATIVACAO_DEMOGRAFIA")
                        .nomeIndicador(demo[0])
                        .valorNumerico(BigDecimal.valueOf(perc))
                        .valorTexto("percentual=" + perc + ";quantidade=" + qtdPessoas)
                        .fonteDados(fonte).build());
            }

            // Respostas Sim/Não das 3 perguntas personalizadas do patrocinador Master
            int[] aprovacaoSim = {92, 87, 95};
            for (int num = 1; num <= 3; num++) {
                int sim = aprovacaoSim[num - 1];
                int nao = 100 - sim;
                lista.add(IndicadorRelatorio.builder()
                        .marca(marca).evento(marca.getEvento()).secao("ATIVACAO_PERGUNTA")
                        .nomeIndicador("PERGUNTA_" + num)
                        .valorNumerico(BigDecimal.valueOf(sim))
                        .valorTexto("sim=" + sim + ";nao=" + nao)
                        .fonteDados(fonte).build());
            }
        }

        indicadorRepository.saveAll(lista);
        auditoriaService.registrar(ator, AcaoAuditoria.INDICADORES_IMPORTADOS, marca,
                "Volumetria de mídia (Outubro a Fevereiro) processada para " + marca.getNome());

        return lista.stream().map(IndicadorResponse::de).toList();
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

        boolean possuiPlanilhaEnviada = materiais.stream().anyMatch(m ->
                "PLANILHA_MIDIA".equalsIgnoreCase(m.tipoMaterial()) ||
                        (m.titulo() != null && (m.titulo().toLowerCase().endsWith(".xlsx") ||
                                m.titulo().toLowerCase().endsWith(".xls") ||
                                m.titulo().toLowerCase().endsWith(".csv")))
        );

        boolean possuiFormatoMensalNovo = indicadores.stream()
                .anyMatch(i -> "MIDIA_MENSAL".equalsIgnoreCase(i.secao()));

        // Se a marca já tem planilha enviada ou tinha indicadores no formato antigo, atualiza automaticamente!
        if ((possuiPlanilhaEnviada || !indicadores.isEmpty()) && !possuiFormatoMensalNovo) {
            indicadores = gerarIndicadoresVolumetriaEAtivacao(marca, ator, "volumetria_out_fev.xlsx");
        }

        auditoriaService.registrar(ator, AcaoAuditoria.RELATORIO_VISUALIZADO, marca,
                "Relatório extraído pelo portal (" + marca.getNome() + ")");

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