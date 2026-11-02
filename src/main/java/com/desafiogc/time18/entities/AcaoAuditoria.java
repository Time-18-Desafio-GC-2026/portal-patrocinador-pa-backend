package com.desafiogc.time18.entities;

/**
 * Catálogo completo de eventos rastreados no Histórico de Auditoria da RBS.
 * Cada evento gera um novo registro imutável na tabela log_auditoria (nunca sobrescreve os anteriores).
 * A coluna "detalhe" da tabela guarda o contexto exato (ex: nome do arquivo, aba, pergunta do FAQ).
 */
public enum AcaoAuditoria {

    // 1. Autenticação, Sessão e Segurança
    LOGIN_REALIZADO,                // Usuário entrou na plataforma
    LOGOUT_REALIZADO,               // Usuário encerrou a sessão
    PRIMEIRO_ACESSO_CONCLUIDO,      // Usuário definiu seu nome e trocou a senha padrão inicial
    SENHA_ALTERADA,                 // Usuário trocou a própria senha voluntariamente
    SENHA_RESETADA,                 // Admin RBS ou Superadmin resetou a senha de um usuário

    // 2. Ciclo de Vida de Usuários (Allow-list: Patrocinadores e Admins RBS)
    USUARIO_CRIADO,                 // Novo e-mail liberado na allow-list
    USUARIO_ATUALIZADO,             // Alteração de nome ou e-mail do usuário
    USUARIO_DESATIVADO,             // Bloqueio temporário de acesso (mantém histórico)
    USUARIO_REATIVADO,              // Desbloqueio de acesso do usuário
    USUARIO_EXCLUIDO,               // Exclusão definitiva de um usuário inativo

    // 3. Ciclo de Vida de Marcas e Cotas (Aba 1 - Perfil do Cliente)
    MARCA_CRIADA,                   // Cadastro de nova marca patrocinadora e sua cota
    MARCA_ATUALIZADA,               // Edição de nome, CNPJ ou nível de cota (Master, Subcota G, Subcota)
    MARCA_SENHA_PADRAO_ALTERADA,    // Admin RBS trocou a senha inicial padrão da marca
    MARCA_DESATIVADA,               // Desativação da marca (revoga o acesso dos patrocinadores dela)
    MARCA_REATIVADA,                // Reativação da marca patrocinadora
    MARCA_EXCLUIDA,                 // Exclusão definitiva de uma marca desativada

    // 4. Gestão e Rastreamento de Materiais e Arquivos (Abas 1, 2, 3, 6 e 7)
    MATERIAL_ENVIADO,               // Upload de arquivo (Plano Comercial, MIV/Logo, Excel, Contrato) ou link
    MATERIAL_VISUALIZADO,           // Usuário abriu/consultou os detalhes ou link de um material
    MATERIAL_BAIXADO,               // Usuário fez o download do arquivo (registra toda vez que baixar!)
    MATERIAL_STATUS_ATUALIZADO,     // Admin RBS mudou o status (ex: de ENVIADO para APROVADO)
    MATERIAL_EXCLUIDO,              // Remoção de um material enviado por engano

    // 5. Operação das Abas Específicas, Relatórios e ETL Python (Abas 3, 4, 5, 6 e 7)
    PERGUNTA_PESQUISA_SALVA,        // Patrocinador cadastrou/editou uma das 3 perguntas (Aba 6)
    CORTESIA_ATUALIZADA,            // Lançamento ou destinação de ingressos/cortesias (Aba 4)
    CREDENCIAMENTO_ACESSADO,        // Consulta de staff ou clique para o Open Staff (Aba 5)
    SIGNATARIO_CONTRATO_SALVO,      // Preenchimento dos dados de quem assina o contrato (Aba 7)
    INDICADORES_IMPORTADOS,         // Planilha Excel processada pelo Python e KPIs gravados no banco
    RELATORIO_VISUALIZADO,          // Usuário abriu o painel/relatório consolidado na tela
    RELATORIO_EXPORTADO,            // Usuário extraiu/baixou o relatório consolidado do patrocinador

    // 6. FAQ Dinâmico e Central de Dúvidas
    DUVIDA_FAQ_ENVIADA,             // Patrocinador enviou uma nova pergunta para a RBS
    DUVIDA_FAQ_RESPONDIDA,          // Admin RBS respondeu e publicou no FAQ (gera alerta global!)
    DUVIDA_FAQ_VISUALIZADA          // Usuário clicou para ler uma resposta do FAQ (alimenta as mais acessadas)
}