#!/usr/bin/env bash
# Sobe o backend local apontando para o Supabase.
# Env vars são lidas de backend/.env.local (crie a partir do .example).
# Uso: ./backend/run-local.sh   (ou passe outro arquivo: ./run-local.sh ~/cliniva.env)
set -euo pipefail

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ENV_FILE="${1:-$DIR/.env.local}"

if [[ -f "$ENV_FILE" ]]; then
  set -a
  # shellcheck disable=SC1090
  source "$ENV_FILE"
  set +a
  echo "→ variáveis carregadas de $ENV_FILE"
else
  echo "⚠ $ENV_FILE não encontrado — copie .env.local.example para .env.local e preencha."
  exit 1
fi

exec "$DIR/mvnw" -q spring-boot:run