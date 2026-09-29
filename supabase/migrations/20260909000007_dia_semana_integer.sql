-- =====================================================================
-- migration 07: corrige o tipo de horario_atendimento.dia_semana
-- Data: 20260909000007
-- Rodar APÓS a 20260909000006_agenda_hardening. Idempotente.
--
-- Motivo: a migration 05 criou `dia_semana smallint` (int2), mas a entidad
-- JPA HorarioAtendimentoId mapeia `Integer diaSemana` (int4). Com
-- `ddl-auto=validate`, o Hibernate recusa subir a aplicação:
--
--   Schema validation: wrong column type encountered in column [dia_semana]
--   in table [horario_atendimento]; found [int2 (Types#SMALLINT)],
--   but expecting [integer (Types#INTEGER)]
--
-- Isso derrubou o deploy do backend no Render logo após o PR #36 (agenda).
-- A correção alinha o banco com o mapeamento Java.
-- =====================================================================

ALTER TABLE horario_atendimento
    ALTER COLUMN dia_semana TYPE integer;

-- a constraint ck_horario_dia_semana continua válida (1..7) após o cast
