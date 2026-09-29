-- =====================================================================
-- migration 06: hardening da agenda (slug, duração, expediente, estoque)
-- Data: 20260909000006
-- Rodar APÓS a 20260909000005_agenda. Idempotente.
-- =====================================================================

-- ---------------------------------------------------------------------
-- 1. Duração máxima: 24h. Impede wrap de LocalTime no cálculo de
--    disponibilidade e valores absurdos vindos da API.
-- ---------------------------------------------------------------------
ALTER TABLE servico DROP CONSTRAINT IF EXISTS ck_servico_duracao_minutos;
ALTER TABLE servico ADD CONSTRAINT ck_servico_duracao_minutos
    CHECK (duracao_minutos BETWEEN 1 AND 1440);

ALTER TABLE atendimento DROP CONSTRAINT IF EXISTS ck_atendimento_duracao_minutos;
ALTER TABLE atendimento ADD CONSTRAINT ck_atendimento_duracao_minutos
    CHECK (duracao_minutos BETWEEN 1 AND 1440);

-- ---------------------------------------------------------------------
-- 2. Backfill da duração real dos atendimentos legados.
--    A migration 05 colocou 30 min em TODOS os atendimentos; o correto é
--    a soma das durações dos serviços já vinculados.
-- ---------------------------------------------------------------------
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

-- ---------------------------------------------------------------------
-- 3. Expediente sempre dentro do mesmo dia (abre < fecha).
-- ---------------------------------------------------------------------
ALTER TABLE horario_atendimento
    DROP CONSTRAINT IF EXISTS ck_horario_janela;
ALTER TABLE horario_atendimento
    ADD CONSTRAINT ck_horario_janela CHECK (abertura < fechamento);

-- ---------------------------------------------------------------------
-- 4. Slug: normaliza de verdade (remove acentos), preenche slugs vazios e
--    resolve colisões com sufixo -2, -3, ...
--
--    A migration 05 gerava o slug com
--    `regexp_replace(nome, '[^a-zA-Z0-9]+', '-', 'g')`, que quebrava acentos
--    ("Clínica Padrão" -> "cl-nica-padr-o") e podia colidir
--    ("A B" e "A-B" -> "a-b"), abortando a migration.
--
--    Aqui usamos unaccent (extensão do Postgres) para o mesmo resultado do
--    `Normalizer` NFD usado no Java (ClinicaProvisioningService).
-- ---------------------------------------------------------------------
CREATE SCHEMA IF NOT EXISTS extensions;

DO $$
BEGIN
    CREATE EXTENSION IF NOT EXISTS unaccent WITH SCHEMA extensions;
EXCEPTION
    WHEN duplicate_object THEN
        -- já existe em outro schema: seguimos usando a função resolúvel
        NULL;
END;
$$;

-- base canônica: sem acentos, minúsculas, só [a-z0-9], sem hífen nas pontas
-- (minúsculo ANTES do replace, senão as maiúsculas viram hífen e
--  "Clínica Padrão" viraria "linica-adrao")
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

-- recria todos os slugs (inclui os quebrados da migration 05), liberando
-- primeiro o índice único parcial para poder colidir durante a reescrita
DROP INDEX IF EXISTS uk_clinica_slug;

UPDATE clinica
SET slug = cliniva_slug_base(nome);

-- resolve colisões com sufixo -2, -3, ... (a clínica mais antiga fica com a base)
WITH ranked AS (
    SELECT id,
           row_number() OVER (PARTITION BY slug ORDER BY criada_em, id) AS pos
    FROM clinica
)
UPDATE clinica c
SET slug = c.slug || CASE WHEN r.pos > 1 THEN '-' || r.pos::text ELSE '' END
FROM ranked r
WHERE c.id = r.id AND r.pos > 1;

-- índice único novamente
CREATE UNIQUE INDEX uk_clinica_slug ON clinica (slug) WHERE slug IS NOT NULL;

-- slug obrigatório: toda clínica tem link público (/agendar/<slug>)
ALTER TABLE clinica ALTER COLUMN slug SET NOT NULL;

-- ---------------------------------------------------------------------
-- 5. Defense-in-depth para estoque: mesmo com lock pessimista na clínica,
--    o banco recusa saldo negativo.
--    NOT VALID não falha se já houver dado inconsistente, mas passa a valer
--    para toda inserção/atualização futura.
-- ---------------------------------------------------------------------
ALTER TABLE item DROP CONSTRAINT IF EXISTS ck_item_quantidade_nao_negativa;
ALTER TABLE item ADD CONSTRAINT ck_item_quantidade_nao_negativa
    CHECK (quantidade >= 0) NOT VALID;

-- ---------------------------------------------------------------------
-- 6. Índice composto para as consultas de agenda (clínica + data),
--    que é o filtro real de listarDia/disponibilidade.
-- ---------------------------------------------------------------------
CREATE INDEX IF NOT EXISTS idx_atendimento_clinica_data
    ON atendimento (clinica_id, data_atendimento);
