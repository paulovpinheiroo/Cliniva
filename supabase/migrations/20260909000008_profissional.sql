-- v0.5.0: dimensão profissional (quem atende), com login opcional
--
-- `profissional` é entidade de apresentação; `usuario` é acesso. São
-- separados de propósito: um profissional pode não ter login nenhum
-- (a clínica cadastra a Ana, a Ana nunca entra no sistema), e um
-- usuário com papel OWNER/ADMIN não precisa ser profissional.
--
-- ⚠️ ESTA MIGRATION NÃO PODE IR PARA `production` SOZINHA.
--
-- Trocar a PK de `horario_atendimento` para incluir `profissional_id`
-- (passo 6) deixa essa coluna NOT NULL — Postgres recusa NULL em coluna
-- que participa da PK. E `AgendaService.semearPadrao` roda em TODO
-- onboarding público, inserindo os 6 dias de expediente de uma clínica nova
-- sem esse campo. Resultado: depois desta migration, `POST /api/public/onboarding`
-- passa a falhar em produção até o código novo subir.
--
-- Ou seja, esta migration e o código do profissional têm que ir juntos, e
-- não dá para usar a ordem "migration → main" da issue #46. A janela sem
-- quebra é curta (o tempo entre os dois deploys) e o onboarding é a porta
-- de aquisição de clientes.
--
-- Como a coluna não pode ser NULL, a ordem interna dos passos importa ainda
-- mais: `profissional_id` precisa existir e estar preenchido ANTES de a PK
-- mudar. Reverter depois disso deixa dado órfão e `DROP COLUMN` em produção
-- não tem volta.
--
-- PLANO DE ROLLOUT (o que vai ser feito, para não depender só de disciplina):
--   1. merge da 08 em `production`  →  banco com a tabela e o backfill
--   2. merge do código na `main`   →  profissional entra no CRUD e no seed
--   3. merge da 09 em `production`  →  SET NOT NULL em atendimento
-- A janela quebrada é entre 1 e 2. O passo 3 é o que fecha de vez.
--
-- Idempotente: existe caminho de aplicação manual documentado no vault.

-- ---------------------------------------------------------------------------
-- 1. Tabela profissional
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS profissional (
    id uuid NOT NULL,
    clinica_id uuid NOT NULL,
    nome varchar(120) NOT NULL,
    cor varchar(7),
    ativo boolean NOT NULL DEFAULT true,
    criado_em timestamp(6) NOT NULL DEFAULT now(),
    CONSTRAINT pk_profissional PRIMARY KEY (id),
    CONSTRAINT fk_profissional_clinica FOREIGN KEY (clinica_id)
        REFERENCES clinica (id) ON DELETE CASCADE,
    CONSTRAINT uk_profissional_nome UNIQUE (clinica_id, nome),
    CONSTRAINT ck_profissional_cor CHECK (cor IS NULL OR cor ~ '^#[0-9A-Fa-f]{6}$'),
    CONSTRAINT ck_profissional_nome CHECK (length(btrim(nome)) > 0)
);

COMMENT ON TABLE profissional IS
    'Pessoa que atende na clínica. Entidade de apresentação, independente de login.';
COMMENT ON COLUMN profissional.cor IS
    'Cor hexadecimal (#RRGGBB) usada na grade por profissional. NULL = o frontend deriva do nome.';
COMMENT ON COLUMN profissional.ativo IS
    'Inativo não aparece em agendamento novo, mas preserva o histórico.';

-- ---------------------------------------------------------------------------
-- 2. Backfill: um profissional "Geral" por clínica COM atendimento ou
--    expediente.
--
--    Precisa acontecer ANTES de criar qualquer coluna NOT NULL, senão os
--    atendimentos que já existem em produção ficam órfãos. O nome "Geral"
--    é o fallback visível na UI quando a clínica ainda não cadastrou
--    ninguém — é o mesmo papel do "clínica monolítica" de antes.
-- ---------------------------------------------------------------------------
INSERT INTO profissional (id, clinica_id, nome, ativo, criado_em)
SELECT gen_random_uuid(),
       c.id,
       'Geral',
       true,
       now()
FROM clinica c
WHERE NOT EXISTS (SELECT 1 FROM atendimento a WHERE a.clinica_id = c.id)
  AND NOT EXISTS (SELECT 1 FROM horario_atendimento h WHERE h.clinica_id = c.id)
ON CONFLICT (clinica_id, nome) DO NOTHING;

-- Clínicas que JÁ têm atendimento/expediente também precisam de um Geral,
-- senão o backfill das colunas abaixo não acha destino.
INSERT INTO profissional (id, clinica_id, nome, ativo, criado_em)
SELECT gen_random_uuid(), c.id, 'Geral', true, now()
FROM clinica c
ON CONFLICT (clinica_id, nome) DO NOTHING;

-- ---------------------------------------------------------------------------
-- 3. atendimento.profissional_id — nullable primeiro
-- ---------------------------------------------------------------------------
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'atendimento' AND column_name = 'profissional_id'
    ) THEN
        ALTER TABLE atendimento ADD COLUMN profissional_id uuid;
    END IF;
END $$;

UPDATE atendimento a
SET profissional_id = p.id
FROM profissional p
WHERE p.clinica_id = a.clinica_id
  AND p.nome = 'Geral'
  AND a.profissional_id IS NULL;

-- ---------------------------------------------------------------------------
-- 4. horario_atendimento.profissional_id — nullable primeiro
--
--    Depois o PK muda para incluir o profissional: hoje a PK é
--    (clinica_id, dia_semana), o que impede dois profissionais com
--    expedientes diferentes no mesmo dia.
-- ---------------------------------------------------------------------------
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'horario_atendimento' AND column_name = 'profissional_id'
    ) THEN
        ALTER TABLE horario_atendimento ADD COLUMN profissional_id uuid;
    END IF;
END $$;

UPDATE horario_atendimento h
SET profissional_id = p.id
FROM profissional p
WHERE p.clinica_id = h.clinica_id
  AND p.nome = 'Geral'
  AND h.profissional_id IS NULL;

-- ---------------------------------------------------------------------------
-- 6. PK de horario_atendimento passa a incluir o profissional
--
--     De (clinica_id, dia_semana) para (clinica_id, profissional_id, dia_semana).
--     É o que permite a Ana trabalhar seg–qua e a Beatriz sex–dom.
--
--     A PK exige que todo NOT NULL de uma coluna que participa dela. Por isso
--     `profissional_id` aqui é NULL e vira NOT NULL na 09 — ver o passo 7.
-- ---------------------------------------------------------------------------
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conrelid = 'horario_atendimento'::regclass
          AND contype = 'p'
          AND pg_get_constraintdef(oid) NOT LIKE '%profissional_id%'
    ) THEN
        IF EXISTS (SELECT 1 FROM horario_atendimento WHERE profissional_id IS NULL) THEN
            RAISE EXCEPTION
                'horario_atendimento tem linhas sem profissional — backfill do passo 4 não cobriu';
        END IF;
        ALTER TABLE horario_atendimento DROP CONSTRAINT pk_horario_atendimento;
        ALTER TABLE horario_atendimento
            ADD CONSTRAINT pk_horario_atendimento PRIMARY KEY (clinica_id, profissional_id, dia_semana);
    END IF;
END $$;

-- ---------------------------------------------------------------------------
-- 7. atendimento.profissional_id fica NULLABLE; a 09 fecha
--
--     NÃO é SET NOT NULL aqui. O código em produção hoje
--     (`AtendimentoService.createAtendimento`) insere atendimento sem esse
--     campo, e com NOT NULL TODO agendamento passaria a falhar até o código
--     novo subir.
--
--     Já `horario_atendimento.profissional_id` é NOT NULL desde o passo 6 —
--     não por escolha, e sim porque a PK o exige. Ver o aviso no topo.
--
--     Até a 09, `atendimento.profissional_id` nulo significa "agendamento
--     legado, sem profissional definido".
-- ---------------------------------------------------------------------------

-- ---------------------------------------------------------------------------
-- 8. FKs com ON DELETE RESTRICT
--
--    Nunca CASCADE em profissional: apagar alguém com histórico de
--    atendimento precisa falhar, não apagar os agendamentos junto.
-- ---------------------------------------------------------------------------
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_atendimento_profissional'
    ) THEN
        ALTER TABLE atendimento
            ADD CONSTRAINT fk_atendimento_profissional FOREIGN KEY (profissional_id)
            REFERENCES profissional (id) ON DELETE RESTRICT;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_horario_profissional'
    ) THEN
        ALTER TABLE horario_atendimento
            ADD CONSTRAINT fk_horario_profissional FOREIGN KEY (profissional_id)
            REFERENCES profissional (id) ON DELETE CASCADE;
    END IF;
END $$;

-- ---------------------------------------------------------------------------
-- 9. N:N serviço ↔ profissional
--
--    Tabela ponte, não coluna em `servico`: uma esteticista faz vários
--    procedimentos e um procedimento pode ser feito por várias.
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS servico_profissional (
    servico_id uuid NOT NULL,
    profissional_id uuid NOT NULL,
    CONSTRAINT pk_servico_profissional PRIMARY KEY (servico_id, profissional_id),
    CONSTRAINT fk_sp_servico FOREIGN KEY (servico_id)
        REFERENCES servico (id) ON DELETE CASCADE,
    CONSTRAINT fk_sp_profissional FOREIGN KEY (profissional_id)
        REFERENCES profissional (id) ON DELETE CASCADE
);

COMMENT ON TABLE servico_profissional IS
    'Quais profissionais executam quais serviços. Vazio = todos podem.';

-- Todo serviço passa a ser executável pelo Geral, para não sumir da
-- disponibilidade enquanto o CRUD de vínculo não existe no código.
INSERT INTO servico_profissional (servico_id, profissional_id)
SELECT s.id, p.id
FROM servico s
JOIN profissional p ON p.clinica_id = s.clinica_id
WHERE NOT EXISTS (
    SELECT 1 FROM servico_profissional sp WHERE sp.servico_id = s.id
)
ON CONFLICT DO NOTHING;

-- ---------------------------------------------------------------------------
-- 10. Índice da consulta de conflito
--
--    Parcial (`status <> 'CANCELADO'`) porque cancelado não bloqueia slot —
--    é o que a lógica já faz. Índice sobre dado que a consulta nunca lê é
--    desperdício de escrita.
-- ---------------------------------------------------------------------------
CREATE INDEX IF NOT EXISTS idx_atendimento_profissional_data
    ON atendimento (profissional_id, data_atendimento)
    WHERE status IS DISTINCT FROM 'CANCELADO';

CREATE INDEX IF NOT EXISTS idx_profissional_clinica_ativo
    ON profissional (clinica_id, ativo);

CREATE INDEX IF NOT EXISTS idx_sp_profissional
    ON servico_profissional (profissional_id);

-- ---------------------------------------------------------------------------
-- 11. Garante que o profissional é da mesma clínica que o serviço
--
--     Sem isto dá para vincular serviço da clínica A a profissional da B
--     e furar o isolamento multi-tenant pela tabela ponte.
--
--     Precisa ser TRIGGER, não CHECK: o Postgres não aceita subquery em
--     CHECK constraint (erro: "cannot use subquery in check constraint").
--     Tentado antes, então fica registrado para ninguém tentar de novo.
-- ---------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION ck_servico_profissional_mesma_clinica()
RETURNS trigger
LANGUAGE plpgsql
AS $$
DECLARE
    clinica_servico uuid;
    clinica_prof uuid;
BEGIN
    SELECT s.clinica_id INTO clinica_servico FROM servico s WHERE s.id = NEW.servico_id;
    SELECT p.clinica_id INTO clinica_prof FROM profissional p WHERE p.id = NEW.profissional_id;

    IF clinica_servico IS NULL OR clinica_prof IS NULL THEN
        RAISE EXCEPTION 'servico_profissional referencia registro inexistente';
    END IF;

    IF clinica_servico <> clinica_prof THEN
        RAISE EXCEPTION
            'servico da clinica % nao pode ser vinculado a profissional da clinica %',
            clinica_servico, clinica_prof;
    END IF;

    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_sp_mesma_clinica ON servico_profissional;
CREATE TRIGGER trg_sp_mesma_clinica
    BEFORE INSERT OR UPDATE ON servico_profissional
    FOR EACH ROW EXECUTE FUNCTION ck_servico_profissional_mesma_clinica();
