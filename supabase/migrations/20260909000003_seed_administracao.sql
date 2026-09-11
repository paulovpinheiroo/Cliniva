-- usuário administrador da plataforma (o vínculo com a identidade do Supabase
-- acontece no primeiro login, quando o supabase_user_id é preenchido)

INSERT INTO usuario (id, papel, ativo, nome, email, criado_em, clinica_id)
VALUES ('00000000-0000-0000-0000-000000000002', 'ADMIN', TRUE, 'Administrador Cliniva',
        'paulovictorpinheiro998663264@gmail.com', now(), NULL);