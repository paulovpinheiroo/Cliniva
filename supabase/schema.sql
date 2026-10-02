-- =====================================================================
-- CLINIVA - Schema completo (v0.4.0 · agenda + resumo do dia)
-- Consolidação: 20260909000001_baseline +
-- 20260909000002_tenant + 20260909000003_seed_administracao +
-- 20260909000004_resumo_dia_cache + 20260909000005_agenda +
-- 20260909000006_agenda_hardening +
-- 20260909000007_dia_semana_integer
-- Para USO MANUAL (SQL Editor do Supabase) em banco NOVO (vazio).
-- =====================================================================

-- ============ DOMÍNIO (baseline) ============

CREATE TABLE cliente (
    id uuid NOT NULL,
    nome varchar(255) NOT NULL,
    email varchar(255),
    telefone varchar(255) NOT NULL,
    data_nascimento date,
    status varchar(255),
    origem varchar(255),
    canal_preferido varchar(255),
    preferencias varchar(255),
    observacoes varchar(255),
    CONSTRAINT pk_cliente PRIMARY KEY (id)
);

CREATE TABLE servico (
    id uuid NOT NULL,
    nome varchar(255) NOT NULL,
    descricao varchar(255),
    valor numeric(38,2) NOT NULL,
    CONSTRAINT pk_servico PRIMARY KEY (id)
);

CREATE TABLE item (
    id uuid NOT NULL,
    nome varchar(255) NOT NULL,
    quantidade numeric(38,2) NOT NULL,
    CONSTRAINT pk_item PRIMARY KEY (id)
);

CREATE TABLE atendimento (
    id uuid NOT NULL,
    data_criacao date NOT NULL,
    data_atendimento timestamp(6) NOT NULL,
    status varchar(255),
    cliente_id uuid,
    CONSTRAINT pk_atendimento PRIMARY KEY (id),
    CONSTRAINT fk_atendimento_cliente FOREIGN KEY (cliente_id) REFERENCES cliente (id)
);

CREATE TABLE atendimento_servico (
    id uuid NOT NULL,
    valor_cobrado numeric(38,2) NOT NULL,
    atendimento_id uuid,
    servico_id uuid,
    CONSTRAINT pk_atendimento_servico PRIMARY KEY (id),
    CONSTRAINT uk_atendimento_servico UNIQUE (atendimento_id, servico_id),
    CONSTRAINT fk_as_atendimento FOREIGN KEY (atendimento_id) REFERENCES atendimento (id),
    CONSTRAINT fk_as_servico FOREIGN KEY (servico_id) REFERENCES servico (id)
);

CREATE TABLE atendimento_item (
    id uuid NOT NULL,
    quantidade_usada numeric(38,2) NOT NULL,
    atendimento_servico_id uuid,
    item_id uuid,
    CONSTRAINT pk_atendimento_item PRIMARY KEY (id),
    CONSTRAINT uk_atendimento_item UNIQUE (atendimento_servico_id, item_id),
    CONSTRAINT fk_ai_atendimento_servico FOREIGN KEY (atendimento_servico_id) REFERENCES atendimento_servico (id),
    CONSTRAINT fk_ai_item FOREIGN KEY (item_id) REFERENCES item (id)
);

CREATE TABLE cliente_nota (
    id uuid NOT NULL,
    texto varchar(255) NOT NULL,
    criada_em timestamp(6) NOT NULL,
    cliente_id uuid NOT NULL,
    CONSTRAINT pk_cliente_nota PRIMARY KEY (id),
    CONSTRAINT fk_nota_cliente FOREIGN KEY (cliente_id) REFERENCES cliente (id)
);

CREATE UNIQUE INDEX uk_cliente_email ON cliente (email);
CREATE UNIQUE INDEX uk_cliente_telefone ON cliente (telefone);
CREATE UNIQUE INDEX uk_servico_nome ON servico (nome);
CREATE UNIQUE INDEX uk_item_nome ON item (nome);

-- ============ ADMINISTRAÇÃO / MULTI-TENANT (tenant) ============

CREATE TABLE clinica (
    id uuid NOT NULL,
    nome varchar(255) NOT NULL,
    ativa boolean NOT NULL,
    criada_em timestamp(6) NOT NULL,
    CONSTRAINT pk_clinica PRIMARY KEY (id),
    CONSTRAINT uk_clinica_nome UNIQUE (nome)
);

CREATE TABLE usuario (
    id uuid NOT NULL,
    supabase_user_id varchar(255),
    papel varchar(255) NOT NULL,
    ativo boolean NOT NULL,
    nome varchar(255),
    email varchar(255) NOT NULL,
    criado_em timestamp(6) NOT NULL,
    clinica_id uuid,
    CONSTRAINT pk_usuario PRIMARY KEY (id),
    CONSTRAINT uk_usuario_email UNIQUE (email),
    CONSTRAINT uk_usuario_supabase UNIQUE (supabase_user_id),
    CONSTRAINT fk_usuario_clinica FOREIGN KEY (clinica_id) REFERENCES clinica (id)
);

-- clínica padrão que absorve os registros já existentes
INSERT INTO clinica (id, nome, ativa, criada_em)
VALUES ('00000000-0000-0000-0000-000000000001', 'Clínica Padrão', TRUE, now());

ALTER TABLE cliente ADD COLUMN clinica_id uuid;
ALTER TABLE servico ADD COLUMN clinica_id uuid;
ALTER TABLE item ADD COLUMN clinica_id uuid;
ALTER TABLE atendimento ADD COLUMN clinica_id uuid;

UPDATE cliente SET clinica_id = '00000000-0000-0000-0000-000000000001';
UPDATE servico SET clinica_id = '00000000-0000-0000-0000-000000000001';
UPDATE item SET clinica_id = '00000000-0000-0000-0000-000000000001';
UPDATE atendimento SET clinica_id = '00000000-0000-0000-0000-000000000001';

ALTER TABLE cliente ALTER COLUMN clinica_id SET NOT NULL;
ALTER TABLE servico ALTER COLUMN clinica_id SET NOT NULL;
ALTER TABLE item ALTER COLUMN clinica_id SET NOT NULL;
ALTER TABLE atendimento ALTER COLUMN clinica_id SET NOT NULL;

ALTER TABLE cliente ADD CONSTRAINT fk_cliente_clinica FOREIGN KEY (clinica_id) REFERENCES clinica (id);
ALTER TABLE servico ADD CONSTRAINT fk_servico_clinica FOREIGN KEY (clinica_id) REFERENCES clinica (id);
ALTER TABLE item ADD CONSTRAINT fk_item_clinica FOREIGN KEY (clinica_id) REFERENCES clinica (id);
ALTER TABLE atendimento ADD CONSTRAINT fk_atendimento_clinica FOREIGN KEY (clinica_id) REFERENCES clinica (id);

-- unicidade global vira unicidade POR CLÍNICA
DROP INDEX uk_cliente_email;
DROP INDEX uk_cliente_telefone;
DROP INDEX uk_servico_nome;
DROP INDEX uk_item_nome;

CREATE UNIQUE INDEX uk_cliente_email_clinica ON cliente (clinica_id, email) WHERE email IS NOT NULL;
CREATE UNIQUE INDEX uk_cliente_telefone_clinica ON cliente (clinica_id, telefone);
CREATE UNIQUE INDEX uk_servico_nome_clinica ON servico (clinica_id, nome);
CREATE UNIQUE INDEX uk_item_nome_clinica ON item (clinica_id, nome);

CREATE INDEX idx_cliente_clinica ON cliente (clinica_id);
CREATE INDEX idx_servico_clinica ON servico (clinica_id);
CREATE INDEX idx_item_clinica ON item (clinica_id);
CREATE INDEX idx_atendimento_clinica ON atendimento (clinica_id);

-- ============ SEED ADMINISTRAÇÃO ============

-- usuário administrador da plataforma (o vínculo com a identidade do Supabase
-- acontece no primeiro login, quando o supabase_user_id é preenchido)
INSERT INTO usuario (id, papel, ativo, nome, email, criado_em, clinica_id)
VALUES ('00000000-0000-0000-0000-000000000002', 'ADMIN', TRUE, 'Administrador Cliniva',
        'paulovictorpinheiro998663264@gmail.com', now(), NULL);

-- ============ RESUMO DO DIA (migration 04) ============

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

-- ============ AGENDA (migration 05) ============

ALTER TABLE servico ADD COLUMN duracao_minutos integer NOT NULL DEFAULT 30;
ALTER TABLE servico ADD CONSTRAINT ck_servico_duracao_minutos CHECK (duracao_minutos > 0);

ALTER TABLE atendimento ADD COLUMN duracao_minutos integer NOT NULL DEFAULT 30;
ALTER TABLE atendimento ADD CONSTRAINT ck_atendimento_duracao_minutos CHECK (duracao_minutos > 0);
CREATE INDEX idx_atendimento_data_atendimento ON atendimento (data_atendimento);

ALTER TABLE clinica ADD COLUMN slug text;
CREATE UNIQUE INDEX uk_clinica_slug ON clinica (slug) WHERE slug IS NOT NULL;

UPDATE clinica
SET slug = lower(regexp_replace(trim(nome), '[^a-zA-Z0-9]+', '-', 'g'))
WHERE slug IS NULL;

CREATE TABLE horario_atendimento (
    clinica_id uuid NOT NULL,
    dia_semana integer NOT NULL,
    abertura time NOT NULL,
    fechamento time NOT NULL,
    ativo boolean NOT NULL DEFAULT true,
    CONSTRAINT pk_horario_atendimento PRIMARY KEY (clinica_id, dia_semana),
    CONSTRAINT fk_horario_atendimento_clinica FOREIGN KEY (clinica_id)
        REFERENCES clinica (id) ON DELETE CASCADE,
    CONSTRAINT ck_horario_dia_semana CHECK (dia_semana BETWEEN 1 AND 7)
);

INSERT INTO horario_atendimento (clinica_id, dia_semana, abertura, fechamento)
SELECT c.id, d.dia, '08:00', '18:00'
FROM clinica c
CROSS JOIN generate_series(1, 6) AS d(dia)
ON CONFLICT (clinica_id, dia_semana) DO NOTHING;

-- ============ AGENDA — HARDENING (migration 06) ============

-- duração máxima: 24h (impede wrap de LocalTime e abuse via serviço)
ALTER TABLE servico DROP CONSTRAINT IF EXISTS ck_servico_duracao_minutos;
ALTER TABLE servico ADD CONSTRAINT ck_servico_duracao_minutos
    CHECK (duracao_minutos BETWEEN 1 AND 1440);

ALTER TABLE atendimento DROP CONSTRAINT IF EXISTS ck_atendimento_duracao_minutos;
ALTER TABLE atendimento ADD CONSTRAINT ck_atendimento_duracao_minutos
    CHECK (duracao_minutos BETWEEN 1 AND 1440);

-- backfill da duração real dos atendimentos legados (soma dos serviços)
UPDATE atendimento a
SET duracao_minutos = sub.total
FROM (
    SELECT ats.atendimento_id, SUM(s.duracao_minutos)::int AS total
    FROM atendimento_servico ats
    JOIN servico s ON s.id = ats.servico_id
    GROUP BY ats.atendimento_id
    HAVING SUM(s.duracao_minutos) > 30
) sub
WHERE a.id = sub.atendimento_id
  AND a.duracao_minutos = 30;

-- janela do expediente sempre dentro do mesmo dia
ALTER TABLE horario_atendimento
    ADD CONSTRAINT ck_horario_janela CHECK (abertura < fechamento);

-- normalização real de slug (remove acentos), com resolução de colisões
CREATE SCHEMA IF NOT EXISTS extensions;
CREATE EXTENSION IF NOT EXISTS unaccent WITH SCHEMA extensions;

CREATE OR REPLACE FUNCTION cliniva_slug_base(nome text)
RETURNS text
LANGUAGE sql
IMMUTABLE
AS $$
    SELECT COALESCE(
        NULLIF(
            trim(BOTH '-' FROM
                regexp_replace(
                    lower(COALESCE(extensions.unaccent(nome), nome)),
                    '[^a-z0-9]+', '-', 'g'
                )
            ),
            ''
        ),
        'clinica'
    );
$$;

DROP INDEX IF EXISTS uk_clinica_slug;
UPDATE clinica SET slug = cliniva_slug_base(nome);

WITH ranked AS (
    SELECT id,
           row_number() OVER (PARTITION BY slug ORDER BY criada_em, id) AS pos
    FROM clinica
)
UPDATE clinica c
SET slug = c.slug || CASE WHEN r.pos > 1 THEN '-' || r.pos::text ELSE '' END
FROM ranked r
WHERE c.id = r.id AND r.pos > 1;

CREATE UNIQUE INDEX uk_clinica_slug ON clinica (slug) WHERE slug IS NOT NULL;

-- slug passa a ser obrigatório (todas as clínicas têm link público)
ALTER TABLE clinica ALTER COLUMN slug SET NOT NULL;

-- defense-in-depth: mesmo com lock pessimista, o banco recusa estoque negativo
ALTER TABLE item DROP CONSTRAINT IF EXISTS ck_item_quantidade_nao_negativa;
ALTER TABLE item ADD CONSTRAINT ck_item_quantidade_nao_negativa
    CHECK (quantidade >= 0) NOT VALID;

-- índice composto para as consultas de agenda (clínica + data)
CREATE INDEX IF NOT EXISTS idx_atendimento_clinica_data
    ON atendimento (clinica_id, data_atendimento);
