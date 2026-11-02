-- 4. Tabela de Materiais e Arquivos (Atende envios, downloads e consultas das Abas 1, 2, 3, 6 e 7)
CREATE TABLE material (
                          id               UUID         PRIMARY KEY,
                          marca_id         UUID         NOT NULL REFERENCES marca (id) ON DELETE CASCADE,
                          titulo           VARCHAR(150) NOT NULL,
                          descricao        VARCHAR(500),
                          aba_destino      VARCHAR(30)  NOT NULL,
                          tipo_material    VARCHAR(30)  NOT NULL,
                          caminho_storage  VARCHAR(500),
                          url_externa      VARCHAR(500),
                          status           VARCHAR(30)  NOT NULL DEFAULT 'ENVIADO',
                          autor_id         UUID         NOT NULL,
                          autor_nome       VARCHAR(150) NOT NULL,
                          autor_perfil     VARCHAR(20)  NOT NULL,
                          criado_em        TIMESTAMPTZ  NOT NULL,
                          atualizado_em    TIMESTAMPTZ  NOT NULL
);

CREATE INDEX idx_material_marca ON material (marca_id, atualizado_em DESC);

-- 5. Tabela de Perguntas da Pesquisa de Ativação (Aba 6 - até 3 perguntas por marca)
CREATE TABLE pergunta_pesquisa (
                                   id               UUID         PRIMARY KEY,
                                   marca_id         UUID         NOT NULL REFERENCES marca (id) ON DELETE CASCADE,
                                   numero_pergunta  INT          NOT NULL,
                                   texto_pergunta   VARCHAR(500) NOT NULL,
                                   autor_nome       VARCHAR(150) NOT NULL,
                                   atualizado_em    TIMESTAMPTZ  NOT NULL,
                                   CONSTRAINT chk_numero_pergunta CHECK (numero_pergunta BETWEEN 1 AND 3),
                                   CONSTRAINT uq_marca_numero_pergunta UNIQUE (marca_id, numero_pergunta)
);

-- 6. Tabela de Indicadores e Resultados Consolidados (Aba 3 e Relatório V1)
CREATE TABLE indicador_relatorio (
                                     id               UUID           PRIMARY KEY,
                                     marca_id         UUID           NOT NULL REFERENCES marca (id) ON DELETE CASCADE,
                                     evento           VARCHAR(100)   NOT NULL DEFAULT 'Planeta Atlântida 2026',
                                     secao            VARCHAR(50)    NOT NULL,
                                     nome_indicador   VARCHAR(150)   NOT NULL,
                                     valor_numerico   NUMERIC(15, 2),
                                     valor_texto      VARCHAR(500),
                                     fonte_dados      VARCHAR(150)   NOT NULL,
                                     atualizado_em    TIMESTAMPTZ    NOT NULL
);

CREATE INDEX idx_indicador_marca ON indicador_relatorio (marca_id, secao);

-- 7. Tabela de FAQ Dinâmico e Alertas Globais (Problema 04 do TAP)
CREATE TABLE duvida_faq (
                            id                 UUID         PRIMARY KEY,
                            pergunta           VARCHAR(500) NOT NULL,
                            resposta           TEXT,
                            categoria          VARCHAR(50)  NOT NULL,
                            status             VARCHAR(20)  NOT NULL DEFAULT 'PENDENTE',
                            publica            BOOLEAN      NOT NULL DEFAULT FALSE,
                            visualizacoes      INT          NOT NULL DEFAULT 0,
                            autor_pergunta_id  UUID,
                            autor_marca_id     UUID         REFERENCES marca (id) ON DELETE SET NULL,
                            respondido_por     VARCHAR(150),
                            criado_em          TIMESTAMPTZ  NOT NULL,
                            respondido_em      TIMESTAMPTZ
);

CREATE INDEX idx_faq_publica_acessos ON duvida_faq (publica, visualizacoes DESC);
CREATE INDEX idx_faq_alertas_recentes ON duvida_faq (publica, respondido_em DESC);