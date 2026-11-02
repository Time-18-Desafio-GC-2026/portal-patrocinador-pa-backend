package com.desafiogc.time18.services;

import com.desafiogc.time18.common.ExcecaoApi;
import com.desafiogc.time18.dtos.OperacaoPortalDtos.*;
import com.desafiogc.time18.entities.*;
import com.desafiogc.time18.repositories.IndicadorRelatorioRepository;
import com.desafiogc.time18.repositories.MaterialRepository;
import com.desafiogc.time18.repositories.PerguntaPesquisaRepository;
import com.desafiogc.time18.security.UsuarioAutenticado;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;


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

    // ==================== 1. MATERIAIS E ARQUIVOS (CRITÉRIO DE ACEITE V1) ====================

    public MaterialResponse registrarMaterial(UUID marcaId, RegistrarMaterialRequest req, MultipartFile arquivo, UsuarioAutenticado logado) {
        Usuario ator = autenticacaoService.buscarUsuarioAtivo(logado);
        Marca marca = marcaService.buscarEValidarAcessoMarca(marcaId, ator);

        String urlExterna = req.urlExterna();

        // Se houver um arquivo na requisição, salva na nuvem e substitui a urlExterna pelo link público
        if (arquivo != null && !arquivo.isEmpty()) {
            urlExterna = storageService.fazerUpload(arquivo, "marcas/" + marcaId);
        }

        Material material = Material.builder()
                .marca(marca)
                .titulo(req.titulo().trim())
                .descricao(req.descricao())
                .abaDestino(req.abaDestino().trim().toUpperCase())
                .tipoMaterial(req.tipoMaterial().trim().toUpperCase())
                .caminhoStorage(req.caminhoStorage())
                .urlExterna(urlExterna) // Guardará a URL real do Supabase
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

        // Audita toda vez que o arquivo é aberto/baixado sem apagar os registros anteriores!
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

    // ==================== 3. RELATÓRIO CONSOLIDADO V1 E INTEGRAÇÃO DADOS ====================

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

    /**
     * Carrega automaticamente na tabela indicador_relatorio os KPIs consolidados da planilha oficial
     * resultados_cliente.xlsx (Mídia + Pesquisa de Ativação) para demonstração imediata!
     */
    public List<IndicadorResponse> importarAmostraPadraoPlanilha(UUID marcaId, UsuarioAutenticado logado) {
        Usuario admin = autenticacaoService.buscarUsuarioAtivo(logado);
        Marca marca = marcaService.buscarEValidarAcessoMarca(marcaId, admin);

        indicadorRepository.deleteByMarcaIdAndSecao(marcaId, "MIDIA");
        indicadorRepository.deleteByMarcaIdAndSecao(marcaId, "PESQUISA_ATIVACAO");

        List<IndicadorRelatorio> amostra = List.of(
                criarInd(marca, "MIDIA", "Instagram - Visualizações (30 e 31/01)", 190000, "80k no dia 30/01 + 110k no dia 31/01", "Planilha resultados_cliente.xlsx (Aba Mídia)"),
                criarInd(marca, "MIDIA", "TikTok - Visualizações (30 e 31/01)", 150000, "60k no dia 30/01 + 90k no dia 31/01", "Planilha resultados_cliente.xlsx (Aba Mídia)"),
                criarInd(marca, "MIDIA", "YouTube - Visualizações (30 e 31/01)", 80000, "30k no dia 30/01 + 50k no dia 31/01", "Planilha resultados_cliente.xlsx (Aba Mídia)"),
                criarInd(marca, "MIDIA", "TikTok - Menções Orgânicas", 420, "180 no dia 30/01 + 240 no dia 31/01", "Planilha resultados_cliente.xlsx (Aba Mídia)"),
                criarInd(marca, "MIDIA", "Imprensa - Citações Oficiais", 30, "12 no dia 30/01 + 18 no dia 31/01", "Planilha resultados_cliente.xlsx (Aba Mídia)"),
                criarInd(marca, "PESQUISA_ATIVACAO", "Importância das Marcas no Festival (Q07)", 88, "176 de 200 respondentes avaliaram como importante/muito importante", "Planilha resultados_cliente.xlsx (Aba Pesquisa)"),
                criarInd(marca, "PESQUISA_ATIVACAO", "Diversão na Ativação da Marca (Q08)", 74, "148 de 200 visitantes afirmaram ter se divertido na ativação", "Planilha resultados_cliente.xlsx (Aba Pesquisa)"),
                criarInd(marca, "PESQUISA_ATIVACAO", "Inclusão e Acessibilidade (Q09)", 77, "154 de 200 visitantes consideram a experiência inclusiva", "Planilha resultados_cliente.xlsx (Aba Pesquisa)")
        );

        indicadorRepository.saveAll(amostra);
        auditoriaService.registrar(admin, AcaoAuditoria.INDICADORES_IMPORTADOS, marca,
                "Importação automática da base resultados_cliente.xlsx concluída (8 indicadores consolidados)");

        return amostra.stream().map(IndicadorResponse::de).toList();
    }

    /**
     * Gera o Relatório Consolidado V1 do Patrocinador direto da base (sem montagem manual!).
     */
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

    private IndicadorRelatorio criarInd(Marca marca, String secao, String nome, double valor, String texto, String fonte) {
        return IndicadorRelatorio.builder()
                .marca(marca)
                .evento(marca.getEvento())
                .secao(secao)
                .nomeIndicador(nome)
                .valorNumerico(BigDecimal.valueOf(valor))
                .valorTexto(texto)
                .fonteDados(fonte)
                .build();
    }
}