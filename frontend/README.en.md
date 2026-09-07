# Clíniva — Frontend

Web UI (SPA) for the Clíniva management system — dashboard, clients,
services, stock and appointments, built with **React + TypeScript + Vite +
Tailwind CSS v4**.

> Portuguese version: [`README.md`](./README.md)

## Stack

| Layer | Technology |
|-------|------------|
| UI | React 19 + React Router 7 |
| Language | TypeScript (~5/6) |
| Build | Vite 8 |
| Styling | Tailwind CSS v4 (`@tailwindcss/vite`) |
| Lint | Oxlint |
| Tests | — (planned) |

## Commands

```bash
npm install          # install dependencies
npm run dev          # dev server at http://localhost:5173 (HMR)
npm run build        # tsc -b && vite build → dist/
npm run lint         # oxlint
npm run preview      # serve dist/
```

During development the backend must be running on `localhost:8080` (see
[`../backend/README.en.md`](../backend/README.en.md)). Vite proxies
`/api/*` to it (`vite.config.ts`), so there is no CORS or environment
setup on the frontend.

## `src/` layout

```
src/
├── main.tsx                   Bootstrap (ReactDOM + BrowserRouter)
├── App.tsx                    Routes (all inside <AppLayout/>)
├── index.css                  Theme tokens, typography, animations
├── pages/                     One screen per route
│   ├── DashboardPage.tsx      Counts, upcoming appointments, low stock
│   ├── ClientesPage.tsx       Client list + CRUD
│   ├── ServicosPage.tsx       Service catalogue + CRUD
│   ├── EstoquePage.tsx        Items + stock movements
│   └── AtendimentosPage.tsx   Schedule + extra items + status
├── components/
│   ├── layout/AppLayout.tsx   Fixed carbon rail + ivory content
│   └── ui/                    Editorial primitives
│       ├── Button.tsx, TextField.tsx, Select.tsx
│       ├── Modal.tsx, ConfirmDialog.tsx
│       ├── PageHeader.tsx, StatusBadge.tsx
│       ├── Spinner.tsx, EmptyState.tsx, ErrorBanner.tsx
├── api/                       HTTP layer
│   ├── http.ts                fetch wrapper + ApiError
│   └── *Api.ts                One typed module per domain
├── hooks/
│   ├── useApi.ts              data/loading/error state + refetch
│   └── useTheme.ts            Light/dark theme (persisted in localStorage)
├── types/index.ts             API DTO types
└── utils/format.ts            Formatting (BRL, dates, relative dates)
```

## Editorial conventions

The UI follows the editorial visual language:

- **Typography**: Fraunces (display/headings), Inter (body), IBM Plex Mono
  (dates, quantities, labels).
- **Labels/badges**: lowercase, `uppercase`, `0.18em` tracking, 10–13px body.
- **Shapes**: `rounded-none` (no border radius) across the whole layout.
- **Shadows**: none — depth comes from 1px hairlines and adjacent tones.
- **Interaction**: `transition-colors duration-150 ease-in-out`; buttons have
  a tactile press (`enabled:active:translate-y-px`).
- **Rail**: fixed dark carbon, accent on the active page; brand name
  "Clíniva" is accented.

## Theming (light / dark)

The theme is **tokenized** in `index.css`:

- Semantic tokens declared with `@theme inline` → resolved to CSS vars
  `--cli-*` (`--cli-ivory`, `--cli-paper`, `--cli-ink`, `--cli-ink-soft`,
  `--cli-hairline`, `--cli-accent`, `--cli-accent-strong`).
- `:root` = light; `.dark` = dark (inverted subset).
- **Per-mode accent**: light → sage (`#8BAE9A`), dark → lilac (`#B58CFF`).
- Fixed palette (sage/lilac/purple/carbon/bone) covers **semantic**
  elements that must not follow the theme (status, stock quantities, rail text).
- `useTheme` toggles and persists to `localStorage` (`cliniva-theme`); the
  first visit follows `prefers-color-scheme` via an inline anti-FOUC script
  in `index.html`.

## Animation system

Sober, dependency-free motion:

- `animate-rise` (200ms), `animate-fade` (140ms), `animate-stagger`
  (240ms) — easing `cubic-bezier(.2, .8, .2, 1)`.
- Page transitions via `PageTransition` (key = pathname) in the layout.
- Modals render **through a portal** into `document.body` and blur the whole
  app (`cdk-blur-open #root { filter: blur(6px) }`) — uniform in both themes.
- `prefers-reduced-motion: reduce` disables every animation.

## Responsive / mobile

- **Drawer**: below `lg`, the carbon rail becomes an off-canvas drawer
  opened by the `[ menu ]` button in the header; closes on navigation,
  `Escape` or clicking the darkened overlay.
- **Card lists**: below `md`, the record screens (clients, services, stock,
  appointments) swap their table for stackable cards
  (`CardList`/`CardItem`/`CardDetail`/`CardActions` in
  `components/ui/CardList.tsx`).
- **Touch**: buttons and form fields get a taller touch target on mobile
  (`py-2.5`/`py-3`), back to compact on `md+`.
- **Adaptation**: `PageHeader`, modals, header and footer adjust padding,
  heading size and date (short on mobile) per breakpoint.

## Adding a page/route

1. Create `src/pages/NovaPaginaPage.tsx` following the existing page pattern
   (`useApi` + `PageHeader` + `ui/` primitives).
2. Register the route in `src/App.tsx`, inside
   `<Route element={<AppLayout />}>`.
3. Add the rail entry in `src/components/layout/AppLayout.tsx`
   (title, number, path); the active badge follows automatically.
4. If the page needs backend data, add a typed `src/api/novaCoisaApi.ts`
   (using `http` + types from `types/index.ts`).

## Utilities

- `utils/format.ts`: `formatCurrency` (BRL), `formatDate`, `formatDateTime`,
  relative dates (e.g. "há 3 dias") used on the dashboard and totals.
- The `@` alias points to `src/` (configured in `vite.config.ts` and tsconfig).