-- 1. Tabela de Marcas Patrocinadoras (Aba 1 do TAP)
CREATE TABLE marca (
                       id                 UUID         PRIMARY KEY,
                       nome               VARCHAR(150) NOT NULL,
                       cnpj               VARCHAR(14)  NOT NULL UNIQUE,
                       cota               VARCHAR(20)  NOT NULL,
                       evento             VARCHAR(100) NOT NULL DEFAULT 'Planeta Atlântida 2026',
                       senha_padrao_hash  VARCHAR(100) NOT NULL,
                       ativa              BOOLEAN      NOT NULL DEFAULT TRUE,
                       criado_em          TIMESTAMPTZ  NOT NULL,
                       atualizado_em      TIMESTAMPTZ  NOT NULL,
    -- Garante no banco que a cota seja apenas uma das 3 previstas no TAP da RBS
                       CONSTRAINT chk_marca_cota CHECK (cota IN ('MASTER', 'SUB_COTA_G', 'SUB_COTA')),
    -- Garante que o CNPJ tenha exatamente 14 dígitos numéricos
                       CONSTRAINT chk_marca_cnpj CHECK (cnpj ~ '^[0-9]{14}$')
    );

-- 2. Tabela de Usuários (Allow-list de e-mails liberados pela RBS)
CREATE TABLE usuario (
                         id                   UUID         PRIMARY KEY,
                         email                VARCHAR(254) NOT NULL UNIQUE,
                         nome                 VARCHAR(150),
                         senha_hash           VARCHAR(100) NOT NULL,
                         perfil               VARCHAR(20)  NOT NULL,
                         marca_id             UUID         REFERENCES marca (id) ON DELETE CASCADE,
                         ativo                BOOLEAN      NOT NULL DEFAULT TRUE,
                         precisa_trocar_senha BOOLEAN      NOT NULL DEFAULT TRUE,
                         primeiro_acesso_em   TIMESTAMPTZ,
                         criado_em            TIMESTAMPTZ  NOT NULL,
                         atualizado_em        TIMESTAMPTZ  NOT NULL,
                         CONSTRAINT chk_usuario_perfil CHECK (perfil IN ('SUPERADMIN', 'ADMIN', 'PATROCINADOR')),
    -- Regra de ouro de isolamento: Todo PATROCINADOR é obrigado a ter marca_id; ADMIN nunca tem marca_id
                         CONSTRAINT chk_usuario_marca CHECK (
                             (perfil = 'PATROCINADOR' AND marca_id IS NOT NULL) OR (perfil <> 'PATROCINADOR' AND marca_id IS NULL)
                             )
);

-- Impede que exista mais de um SUPERADMIN no banco
CREATE UNIQUE INDEX uq_unico_superadmin ON usuario (perfil) WHERE perfil = 'SUPERADMIN';
CREATE INDEX idx_usuario_marca ON usuario (marca_id);

-- 3. Tabela de Histórico / Log de Auditoria (Sem chave estrangeira para preservar histórico mesmo após exclusões)
CREATE TABLE log_auditoria (
                               id               VARCHAR(7)   PRIMARY KEY,
                               ator_id          UUID         NOT NULL,
                               ator_nome        VARCHAR(254) NOT NULL,
                               ator_perfil      VARCHAR(20)  NOT NULL,
                               ator_marca_nome  VARCHAR(150),
                               acao             VARCHAR(40)  NOT NULL,
                               marca_id         UUID,
                               marca_nome       VARCHAR(150),
                               detalhe          VARCHAR(255),
                               criado_em        TIMESTAMPTZ  NOT NULL
);

CREATE INDEX idx_log_auditoria_criado ON log_auditoria (criado_em DESC);
CREATE INDEX idx_log_auditoria_marca  ON log_auditoria (marca_id, criado_em DESC);