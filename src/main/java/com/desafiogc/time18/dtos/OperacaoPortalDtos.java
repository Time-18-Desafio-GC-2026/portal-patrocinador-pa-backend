package com.desafiogc.time18.dtos;

import com.desafiogc.time18.entities.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;


public final class OperacaoPortalDtos {

    
    public record RegistrarMaterialRequest(
            @NotBlank(message = "Informe o título do material.")
            String titulo,

            String descricao,

            @NotBlank(message = "Informe a aba de destino (ex: ABA_1_PERFIL, ABA_2_MARCA, ABA_3_MIDIA, ABA_7_CONTRATO).")
            String abaDestino,

            @NotBlank(message = "Informe o tipo (ex: PLANO_COMERCIAL, MIV_LOGO, PLANILHA_MIDIA, LINK_POST, CONTRATO).")
            String tipoMaterial,

            String caminhoStorage,
            String urlExterna
    ) {}

    public record AtualizarStatusMaterialRequest(
            @NotBlank(message = "Informe o novo status (ENVIADO, EM_ANALISE, APROVADO, PROCESSADO).")
            String status
    ) {}

    public record MaterialResponse(
            UUID id,
            UUID marcaId,
            String marcaNome,
            String titulo,
            String descricao,
            String abaDestino,
            String tipoMaterial,
            String caminhoStorage,
            String urlExterna,
            String status,
            String autorNome,
            Perfil autorPerfil,
            Instant criadoEm,
            Instant atualizadoEm
    ) {
        public static MaterialResponse de(Material m) {
            return new MaterialResponse(
                    m.getId(),
                    m.getMarca().getId(),
                    m.getMarca().getNome(),
                    m.getTitulo(),
                    m.getDescricao(),
                    m.getAbaDestino(),
                    m.getTipoMaterial(),
                    m.getCaminhoStorage(),
                    m.getUrlExterna(),
                    m.getStatus(),
                    m.getAutorNome(),
                    m.getAutorPerfil(),
                    m.getCriadoEm(),
                    m.getAtualizadoEm()
            );
        }
    }

    
    public record SalvarPerguntaPesquisaRequest(
            @NotNull(message = "Informe o número da pergunta (1, 2 ou 3).")
            @Min(value = 1, message = "O número da pergunta deve ser entre 1 e 3.")
            @Max(value = 3, message = "O patrocinador tem direito a no máximo 3 perguntas.")
            Integer numeroPergunta,

            @NotBlank(message = "Digite o texto da pergunta para a pesquisa de ativação.")
            String textoPergunta
    ) {}

    public record PerguntaPesquisaResponse(
            UUID id,
            UUID marcaId,
            String marcaNome,
            Cota marcaCota,
            Integer numeroPergunta,
            String textoPergunta,
            String autorNome,
            Instant atualizadoEm
    ) {
        public static PerguntaPesquisaResponse de(PerguntaPesquisa p) {
            return new PerguntaPesquisaResponse(
                    p.getId(),
                    p.getMarca().getId(),
                    p.getMarca().getNome(),
                    p.getMarca().getCota(),
                    p.getNumeroPergunta(),
                    p.getTextoPergunta(),
                    p.getAutorNome(),
                    p.getAtualizadoEm()
            );
        }
    }

    
    public record SalvarIndicadorRequest(
            @NotBlank(message = "Informe a seção (MIDIA, PESQUISA_ATIVACAO, REDES_SOCIAIS, ESG).")
            String secao,

            @NotBlank(message = "Informe o nome do indicador.")
            String nomeIndicador,

            BigDecimal valorNumerico,
            String valorTexto,

            @NotBlank(message = "Informe a fonte/origem dos dados (ex: Planilha Oficial RBS - Aba Mídia).")
            String fonteDados
    ) {}

    public record IndicadorResponse(
            UUID id,
            String secao,
            String nomeIndicador,
            BigDecimal valorNumerico,
            String valorTexto,
            String fonteDados,
            Instant atualizadoEm
    ) {
        public static IndicadorResponse de(IndicadorRelatorio i) {
            return new IndicadorResponse(
                    i.getId(),
                    i.getSecao(),
                    i.getNomeIndicador(),
                    i.getValorNumerico(),
                    i.getValorTexto(),
                    i.getFonteDados(),
                    i.getAtualizadoEm()
            );
        }
    }

    
    public record RelatorioConsolidadoResponse(
            UUID marcaId,
            String marcaNome,
            Cota cota,
            String evento,
            Instant geradoEm,
            List<MaterialResponse> materiaisEntregues,
            List<PerguntaPesquisaResponse> perguntasPesquisa,
            List<IndicadorResponse> indicadoresConsolidados
    ) {}
}