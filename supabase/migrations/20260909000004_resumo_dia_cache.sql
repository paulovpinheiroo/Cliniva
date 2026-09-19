-- cache do resumo diário com IA: uma linha por clínica/dia guarda o texto gerado
-- e o contador de gerações de LLM (limite diário por clínica)

CREATE TABLE resumo_dia_cache (
    clinica_id uuid NOT NULL,
    data date NOT NULL,
    texto text NOT NULL,
    origem varchar(20) NOT NULL,
    tentativas integer NOT NULL DEFAULT 0,
    gerado_em timestamptz,
    CONSTRAINT pk_resumo_dia_cache PRIMARY KEY (clinica_id, data),
    CONSTRAINT fk_resumo_dia_cache_clinica FOREIGN KEY (clinica_id)
        REFERENCES clinica (id) ON DELETE CASCADE,
    CONSTRAINT ck_resumo_dia_cache_origem CHECK (origem IN ('IA', 'TEMPLATE'))
);