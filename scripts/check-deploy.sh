#!/usr/bin/env bash
# =====================================================================
# Confere se o deploy esperado está no ar.
#
# Existe porque o /actuator/health mente: se o processo morre no startup,
# o Render continua servindo o container ANTIGO e o health check segue
# respondendo 200. Foi assim que dois deploys quebrados passaram
# despercebidos e a feature de agenda ficou 7 dias fora do ar.
#
# Uso:
#   scripts/check-deploy.sh                      # compara com origin/main
#   scripts/check-deploy.sh abc1234              # compara com um commit
#   API=https://api.exemplo.com scripts/check-deploy.sh
# =====================================================================
set -euo pipefail

API="${API:-https://cliniva-hrpj.onrender.com}"
EXPECTED="${1:-}"

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$REPO_ROOT"

if [ -z "$EXPECTED" ]; then
    EXPECTED="$(git fetch origin main --quiet 2>/dev/null && git rev-parse --short origin/main)"
fi
EXPECTED_SHORT="${EXPECTED:0:7}"

echo "==> Alvo: $API"
echo "    commit esperado: $EXPECTED_SHORT"
echo ""

# Deriva o sha completo do valor informado (pode ser curto)
full_sha="$(git rev-parse "$EXPECTED" 2>/dev/null || echo "$EXPECTED")"

echo "==> Health"
# O free tier do Render dorme: a primeira requisição pode levar ~2 min. O
# timeout generoso aqui é proposital, senão todo deploy noturno "falha".
health_code="$(curl -s -o /dev/null -w '%{http_code}' -m 300 "$API/actuator/health" || true)"
if [ "$health_code" != "200" ]; then
    echo "    ERRO: /actuator/health -> ${health_code:-sem resposta}"
    exit 1
fi
echo "    ok (200)"

echo "==> Build em produção"
info="$(curl -s -m 60 "$API/api/saude/build" || true)"
if [ -z "$info" ] || ! echo "$info" | grep -q '"commit"'; then
    echo "    ERRO: /api/saude/build não devolveu o commit — deploy não entrou."
    echo "    resposta: ${info:-vazia}"
    echo ""
    echo "    401 aqui significa que o build em produção ainda NÃO tem o"
    echo "    permitAll de /api/saude/** — é o container antigo servindo."
    echo "    Confira o deploy no Render; o health check 200 não prova nada."
    exit 1
fi
echo "    $info"

running_commit="$(echo "$info" | sed -n 's/.*"commit":"\([^"]*\)".*/\1/p')"
running_short="${running_commit:0:7}"

echo ""
if [ "$running_commit" = "desconhecido" ]; then
    echo "ERRO: o build em produção não sabe o próprio commit."
    echo "      (ENV RENDER_GIT_COMMIT ausente — deploy antigo, sem a rota nova)"
    exit 1
fi

# aceita o prefixo curto do expected
if [ "$running_short" != "$EXPECTED_SHORT" ] && [ "$running_commit" != "$full_sha" ]; then
    echo "ERRO: DEPLOY DIVERGENTE"
    echo "      em produção : $running_short"
    echo "      esperado    : $EXPECTED_SHORT"
    echo ""
    echo "      O health check passa, mas a versão em produção não é a da main."
    echo "      Veja o log do deploy no Render — provavelmente a aplicação"
    echo "      morreu no startup (ddl-auto=validate) e o container antigo segue no ar."
    exit 1
fi

echo "OK: deploy em produção é o commit esperado ($running_short)."
