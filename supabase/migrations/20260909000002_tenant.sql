-- v0.3.0: multi-tenant por clínica + entidades de administração

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