package com.desafiogc.time18.dtos;

import java.util.List;

public final class PythonEtlDtos {

    private PythonEtlDtos() {}

    public record IndicadorEtlDto(
            String secao,
            String nomeIndicador,
            Double valorNumerico,
            String valorTexto,
            String fonteDados
    ) {}

    public record ResumoClienteEtlDto(
            String clienteId,
            String clienteNome,
            String cota,
            String evento,
            Integer edicao,
            String cenario,
            String publicoPesquisa,
            Integer totalRespostas
    ) {}

    public record ResultadoEtlPythonResponse(
            boolean sucesso,
            String mensagem,
            ResumoClienteEtlDto resumoCliente,
            int totalMidiaProcessada,
            int totalPesquisaProcessada,
            List<String> alteracoesAutomaticas,
            List<IndicadorEtlDto> indicadores,
            String arquivoPadronizadoGerado
    ) {}
}