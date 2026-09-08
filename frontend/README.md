# Clíniva — Frontend

Interface web (SPA) do sistema de gestão Clíniva — dashboard, clientes,
serviços, estoque e atendimentos, construída em **React + TypeScript + Vite
+ Tailwind CSS v4**.

> Documentação técnica em inglês: [`README.en.md`](./README.en.md)

## Stack

| Camada | Tecnologia |
|--------|------------|
| UI | React 19 + React Router 7 |
| Linguagem | TypeScript (~5/6) |
| Build | Vite 8 |
| Estilo | Tailwind CSS v4 (`@tailwindcss/vite`) |
| Lint | Oxlint |
| Testes | — (planejado) |

## Comandos

```bash
npm install          # dependências
npm run dev          # dev server em http://localhost:5173 (HMR)
npm run build        # tsc -b && vite build → dist/
npm run lint         # oxlint
npm run preview      # serve dist/
```

Durante o desenvolvimento o backend precisa estar rodando em
`localhost:8080` (ver [`../backend/README.md`](../backend/README.md)).
O Vite encaminha `/api/*` para lá via proxy (`vite.config.ts`), então não há
CORS nem configuração de ambiente no frontend.

## Estrutura do `src/`

```
src/
├── main.tsx                   Bootstrap (ReactDOM + BrowserRouter)
├── App.tsx                    Rotas (tudo dentro de <AppLayout/>)
├── index.css                  Tokens de tema, tipografia, animações
├── pages/                     Uma pasta/tela por rota
│   ├── DashboardPage.tsx      Contagens, próximos atendimentos, estoque baixo
│   ├── ClientesPage.tsx       Lista + CRUD de clientes
│   ├── ServicosPage.tsx       Catálogo + CRUD de serviços
│   ├── EstoquePage.tsx        Itens + movimentação de estoque
│   └── AtendimentosPage.tsx   Agenda + serviços extras + status
├── components/
│   ├── layout/AppLayout.tsx   Rail carbon fixa + conteúdo (ivory)
│   └── ui/                    Primitivos editoriais
│       ├── Button.tsx, TextField.tsx, Select.tsx
│       ├── Modal.tsx, ConfirmDialog.tsx
│       ├── PageHeader.tsx, StatusBadge.tsx
│       ├── Spinner.tsx, EmptyState.tsx, ErrorBanner.tsx
├── api/                       Camada HTTP
│   ├── http.ts                Wrapper fetch + ApiError
│   └── *Api.ts                Um módulo por domínio (tipado)
├── hooks/
│   ├── useApi.ts              Estado data/loading/error + refetch
│   └── useTheme.ts            Tema claro/escuro (persiste em localStorage)
├── types/index.ts             Tipos dos DTOs da API
└── utils/format.ts            Formatação (BRL, datas, datas relativas)
```

## Convenções editoriais

A UI segue a linguagem visual do editorial:

- **Tipografia**: Fraunces (display/títulos), Inter (corpo), IBM Plex Mono
  (datas, quantidades, labels).
- **Labels/selos**: minúsculas, `uppercase`, tracking `0.18em`, corpo 10–13px.
- **Formas**: `rounded-none` (sem bordas arredondadas) em todo o layout.
- **Sombras**: nenhuma — profundidade é feita com hairlines (bordas de 1px)
  e tons adjacentes.
- **Interação**: `transition-colors duration-150 ease-in-out`; botões têm
  press tátil (`enabled:active:translate-y-px`).
- **Rail**: carbon escura fixa, com acento na página ativa; nome da marca
  "Clíniva" com acento.

## Sistema de tema (claro / escuro)

O tema é **tokenizado** em `index.css`:

- Tokens semânticos em `@theme inline` → resolvem para variáveis CSS
  `--cli-*` (`--cli-ivory`, `--cli-paper`, `--cli-ink`, `--cli-ink-soft`,
  `--cli-hairline`, `--cli-accent`, `--cli-accent-strong`).
- `:root` = tema claro; `.dark` = tema escuro (subset invertido).
- **Acento por modo**: claro → sálvia (`#8BAE9A`), escuro → lilás (`#B58CFF`).
- Paleta fixa (sage/lilac/purple/carbon/bone) cobre os usos **semânticos**
  que não devem seguir o tema (status, quantidades de estoque, texto da rail).
- `useTheme` alterna e persiste em `localStorage` (`cliniva-theme`); a
  primeira visita segue `prefers-color-scheme` via script inline em
  `index.html` (anti-FOUC).

## Sistema de animação

Movimento **sóbrio**, sem dependências:

- `animate-rise` (200ms), `animate-fade` (140ms), `animate-stagger`
  (240ms) — easing `cubic-bezier(.2, .8, .2, 1)`.
- Transição entre páginas via `PageTransition` (key = pathname) no layout.
- Modais renderizam **via portal** em `document.body` e borram a app inteira
  (`cdk-blur-open #root { filter: blur(6px) }`) — uniforme nos dois temas.
- `prefers-reduced-motion: reduce` desliga todas as animações.

## Responsividade / mobile

- **Drawer**: abaixo de `lg`, a rail carbon vira uma gaveta off-canvas, aberta
  pelo botão `[ menu ]` no cabeçalho; fecha ao navegar, com `Escape` ou clicando
  no overlay escurecido.
- **Listas em cards**: abaixo de `md`, as telas de registro (clientes,
  serviços, estoque, atendimentos) trocam a tabela por cards empilháveis
  (`CardList`/`CardItem`/`CardDetail`/`CardActions` em
  `components/ui/CardList.tsx`).
- **Toque**: botões e campos de formulário têm área de toque maior no mobile
  (`py-2.5`/`py-3`), compacta novamente no `md+`.
- **Adaptação**: `PageHeader`, modais, cabeçalho e rodapé ajustam padding,
  título e data (curta no mobile) por breakpoint.

## Páginas (v0.2.0)

- **`/` Dashboard** — contagens, próximos atendimentos, estoque baixo,
  novos vs recorrentes no mês e aniversariantes (com saudação via WhatsApp).
- **`/clientes`** — lista com busca, filtro por status e selo de fidelidade
  (novo/recorrente/frequente, calculado dos atendimentos concluídos).
- **`/clientes/:id`** — ficha do cliente: perfil, financeiro (gasto total,
  ticket médio, última visita), histórico de atendimentos, anotações
  (adicionar/excluir) e follow-up via `wa.me`.
- **`/servicos`**, **`/estoque`**, **`/atendimentos`** — CRUDs + agenda.
  Atendimentos agendados têm link **Lembrar** (WhatsApp pré-preenchido).

## Como adicionar uma página/rota

1. Crie `src/pages/NovaPaginaPage.tsx` seguindo o padrão das páginas atuais
   (use `useApi` + `PageHeader` + primitivos de `ui/`).
2. Registre a rota em `src/App.tsx`, dentro de `<Route element={<AppLayout />}>`.
3. Adicione a entrada no rail em `src/components/layout/AppLayout.tsx`
   (título, número, path) e o badge ativo segue automaticamente.
4. Se precisar de dados do backend, crie `src/api/novaCoisaApi.ts`
   tipado com a resposta/erro (`http` + tipos em `types/index.ts`).

## Utilidades

- `utils/format.ts`: `formatCurrency` (BRL), `formatDate`, `formatDateTime`,
  datas relativas (ex.: "há 3 dias") usadas no dashboard e nos totais.
- O alias `@` aponta para `src/` (configurado no `vite.config.ts` e `tsconfig`).