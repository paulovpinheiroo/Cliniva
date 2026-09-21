-- agenda: duração de serviços e atendimentos, slug público por clínica e expediente configurável

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
    dia_semana smallint NOT NULL,
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