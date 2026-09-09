-- Cliniva v0.1.0-v0.2.0 schema (baseline p/ Flyway)

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