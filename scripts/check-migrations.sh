#!/usr/bin/env bash
# =====================================================================
# Verifica as migrations contra um Postgres real subindo a aplicação
# com `ddl-auto=validate`.
#
# Por que isso existe: os testes unitários rodam em H2 com
# `ddl-auto=create-drop`, ou seja, o schema vem do Hibernate e as
# migrations SQL NUNCA são exercitadas. Divergência de tipo entre
# migration e entidade (ex.: smallint vs Integer) só aparece no
# deploy de produção — e o processo morre em silêncio, enquanto o
# health check continua respondendo 200 do container antigo.
#
# Uso (local):  scripts/check-migrations.sh
# Uso (CI):     postgres:16 como service + este script
# =====================================================================
set -euo pipefail

DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-5432}"
DB_NAME="${DB_NAME:-cliniva}"
DB_USER="${DB_USER:-postgres}"
DB_PASSWORD="${DB_PASSWORD:-postgres}"
PORT_APP="${PORT_APP:-8099}"
REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
MIGRATIONS_DIR="$REPO_ROOT/supabase/migrations"
LOG_FILE="${LOG_FILE:-$REPO_ROOT/target/check-migrations.log}"

cd "$REPO_ROOT"

export PGPASSWORD="$DB_PASSWORD"
PSQL=(psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -v ON_ERROR_STOP=1 -q)

echo "==> Aguardando Postgres em $DB_HOST:$DB_PORT ..."
for _ in $(seq 1 60); do
    if "${PSQL[@]}" -c 'SELECT 1' >/dev/null 2>&1; then
        break
    fi
    sleep 1
done
"${PSQL[@]}" -c 'SELECT 1' >/dev/null 2>&1 || {
    echo "ERRO: Postgres não respondeu em $DB_HOST:$DB_PORT"
    exit 1
}
echo "    Postgres ok"

echo "==> Aplicando migrations em ordem"
count=0
for file in "$MIGRATIONS_DIR"/*.sql; do
    name="$(basename "$file")"
    echo "    -> $name"
    "${PSQL[@]}" -f "$file" >/dev/null
    count=$((count + 1))
done
echo "    $count migration(s) aplicada(s)"

# As migrations 01..05 são de criação (tabelas, seeds) e não são meant
# para rodar de novo. Só as de hardening (a partir da 06) se declaram
# idempotentes, porque existe o caminho de aplicação manual documentado
# no vault. Reaplicar só as últimas, e ignore o erro se não houver.
echo "==> Verificando idempotência das migrations de hardening"
mapfile -t hardening < <(printf '%s\n' "$MIGRATIONS_DIR"/*.sql | tail -n +6)
if [ "${#hardening[@]}" -gt 0 ]; then
    for file in "${hardening[@]}"; do
        "${PSQL[@]}" -f "$file" >/dev/null 2>&1 || {
            echo "ERRO: migration de hardening NÃO é idempotente: $(basename "$file")"
            exit 1
        }
        echo "    ok: $(basename "$file")"
    done
else
    echo "    (nenhuma migration de hardening)"
fi

echo "==> Buildando a aplicação"
mkdir -p "$(dirname "$LOG_FILE")"
(cd backend && ./mvnw -q -B -DskipTests package)

echo "==> Subindo a app com ddl-auto=validate contra o schema migrado"
SPRING_DATASOURCE_URL="jdbc:postgresql://$DB_HOST:$DB_PORT/$DB_NAME" \
SPRING_DATASOURCE_USER="$DB_USER" \
SPRING_DATASOURCE_PASSWORD="$DB_PASSWORD" \
SERVER_PORT="$PORT_APP" \
    java -jar backend/target/app.jar >"$LOG_FILE" 2>&1 &
APP_PID=$!

cleanup() {
    if kill -0 "$APP_PID" 2>/dev/null; then
        kill "$APP_PID" 2>/dev/null || true
        wait "$APP_PID" 2>/dev/null || true
    fi
}
trap cleanup EXIT

echo "    aguardando a aplicação subir (timeout 180s) ..."
started=0
for _ in $(seq 1 180); do
    if ! kill -0 "$APP_PID" 2>/dev/null; then
        echo ""
        echo "ERRO: a aplicação MORREU durante o startup."
        echo "---- últimas linhas do log ----"
        tail -40 "$LOG_FILE"
        exit 1
    fi
    code="$(curl -s -o /dev/null -w '%{http_code}' "http://localhost:$PORT_APP/actuator/health" 2>/dev/null || true)"
    if [ "$code" = "200" ]; then
        started=1
        break
    fi
    sleep 1
done

if [ "$started" != "1" ]; then
    echo ""
    echo "ERRO: a aplicação não respondeu 200 em /actuator/health no tempo esperado."
    echo "---- últimas linhas do log ----"
    tail -40 "$LOG_FILE"
    exit 1
fi

echo "    aplicação no ar"

echo "==> Conferindo rotas (o contrato do frontend)"
assert_status() {
    local method="$1" path="$2" expected="$3" desc="$4"
    local actual
    actual="$(curl -s -o /dev/null -w '%{http_code}' \
        -X "$method" "http://localhost:$PORT_APP$path" 2>/dev/null || echo 000)"
    if [ "$actual" != "$expected" ]; then
        echo "    ERRO: $method $path -> $actual (esperado $expected) — $desc"
        exit 1
    fi
    echo "    ok: $method $path -> $actual"
}

# 404 = controller existe, recurso não encontrado (rota pública funcionando)
assert_status GET  /api/public/booking/slug-inexistente/servicos 404 "booking público lê slug"
# 400 = controller existe e valida o corpo
assert_status POST /api/public/booking/slug-inexistente            400 "booking público valida payload"
# 401 = rota existe e exige autenticação
assert_status GET  /api/agenda/horarios                            401 "agenda exige auth"
assert_status GET  /api/agenda/link                                401 "link público exige auth"
assert_status GET  /api/me                                         401 "perfil exige auth"
# 400 = onboarding público acessível e validando
assert_status POST /api/public/onboarding                           400 "onboarding público acessível"
# 200 = rota de build pública (é o que revela deploy falho)
assert_status GET  /api/saude/build                                200 "build info pública"

echo ""
echo "==> TUDO OK: migrations, schema e rotas conferidos."
