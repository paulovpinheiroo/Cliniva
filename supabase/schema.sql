-- =====================================================================
-- CLINIVA - Schema completo (todas as tabelas), v0.3.0
-- Consolidação das migrations: 20260909000001_baseline +
-- 20260909000002_tenant + 20260909000003_seed_administracao.
-- Para USO MANUAL (SQL Editor do Supabase) em banco NOVO(vazio).
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