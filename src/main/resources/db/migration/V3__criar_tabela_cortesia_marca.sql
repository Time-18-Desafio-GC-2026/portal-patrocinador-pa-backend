CREATE TABLE cortesia_marca (
                                id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                marca_id UUID NOT NULL UNIQUE REFERENCES marca(id) ON DELETE CASCADE,
                                arena_passaporte INT NOT NULL DEFAULT 0 CHECK (arena_passaporte >= 0),
                                arena_sexta INT NOT NULL DEFAULT 0 CHECK (arena_sexta >= 0),
                                arena_sabado INT NOT NULL DEFAULT 0 CHECK (arena_sabado >= 0),
                                camarote_passaporte INT NOT NULL DEFAULT 0 CHECK (camarote_passaporte >= 0),
                                camarote_sexta INT NOT NULL DEFAULT 0 CHECK (camarote_sexta >= 0),
                                camarote_sabado INT NOT NULL DEFAULT 0 CHECK (camarote_sabado >= 0),
                                premium_passaporte INT NOT NULL DEFAULT 0 CHECK (premium_passaporte >= 0),
                                premium_sexta INT NOT NULL DEFAULT 0 CHECK (premium_sexta >= 0),
                                premium_sabado INT NOT NULL DEFAULT 0 CHECK (premium_sabado >= 0),
                                lounge_info TEXT NOT NULL DEFAULT '',
                                atualizado_em TIMESTAMPTZ NOT NULL DEFAULT NOW()
);