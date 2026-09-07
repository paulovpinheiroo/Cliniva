# Cliniva

Sistema de gestão para clínica de estética de pequeno porte —
agenda, clientes, serviços, controle de estoque e financeiro.

## Contexto

Desenvolvido para resolver um problema real (sistema de gestão
acessível pra pequenos negócios) e como projeto de evolução técnica
em backend (Java/Spring Boot) e frontend (React).

## Stack

- **Backend:** Java 21, Spring Boot 4.1, PostgreSQL, Maven
- **Frontend:** React 19, TypeScript, Vite, Tailwind CSS
- **Testes backend:** JUnit 5 + Mockito (H2 em memória)
- **Deploy:** Cloud gratuita (a definir — Render/Railway/Fly.io)

## Estrutura do repositório

```
cliniva/
├── backend/       API REST — Java / Spring Boot / PostgreSQL / Maven
│   ├── README.md        Guia completo do backend (PT)
│   └── README.en.md     Technical guide (EN)
├── frontend/      SPA — React + TypeScript + Vite + Tailwind
│   ├── README.md        Guia completo do frontend (PT)
│   └── README.en.md     Technical guide (EN)
├── README.md
└── EDR.png        Diagrama de entidade-relacionamento
```

> Guias detalhados: [backend/README.md](backend/README.md) ·
> [backend/README.en.md](backend/README.en.md) ·
> [frontend/README.md](frontend/README.md) ·
> [frontend/README.en.md](frontend/README.en.md)

## Como rodar localmente

### 1. Requisitos

- Java 21
- Maven 3.9+
- Node.js 20+ (npm)
- PostgreSQL rodando em `localhost:5432`

### 2. Banco de dados

Crie o banco e o usuário (uma única vez):

```bash
sudo -u postgres psql -c "CREATE ROLE cliniva LOGIN PASSWORD 'cliniva';"
sudo -u postgres psql -c "CREATE DATABASE cliniva OWNER cliniva;"
```

As credenciais padrão (`cliniva`/`cliniva`) podem ser sobrescritas
via variáveis de ambiente: `SPRING_DATASOURCE_URL`,
`SPRING_DATASOURCE_USER`, `SPRING_DATASOURCE_PASSWORD`.

### 3. Backend (API em `http://localhost:8080`)

```bash
cd backend
mvn spring-boot:run
```

O Hibernate cria/atualiza as tabelas automaticamente (`ddl-auto=update`).

Executar os testes (59 unit tests, usa H2 em memória — não precisa de banco):

```bash
cd backend
mvn test
```

Detalhes completos (endpoints, regras de domínio, erros):
[`backend/README.md`](backend/README.md).

### 4. Frontend (UI em `http://localhost:5173`)

```bash
cd frontend
npm install
npm run dev
```

O Vite encaminha `/api/*` para o backend (`localhost:8080`) durante o
desenvolvimento, então não é necessário configurar CORS localmente.

Detalhes completos (estrutura, tema, animações, como criar páginas):
[`frontend/README.md`](frontend/README.md).

## API — visão geral

| Recurso | Endpoints |
|---------|-----------|
| Clientes | `GET/POST /api/clientes` · `GET/PUT/DELETE /api/clientes/{id}` · busca por `?nome=` |
| Serviços | `GET/POST /api/servicos` · `GET/PUT/DELETE /api/servicos/{id}` |
| Itens/Estoque | `GET/POST /api/items` · `GET/PUT/DELETE /api/items/{id}` · `PATCH /api/items/{id}/estoque` |
| Atendimentos | `GET/POST /api/atendimentos` · `GET /api/atendimentos/{id}` · `PATCH /{id}/status` |

Filtros de atendimento: `?status=`, `?clienteId=`, `?dataInicio=`, `?dataFim=`.

Erros seguem o formato `{"status", "mensagem", "erros"}` (400/404/409).

## Status

🚀 **v0.1.0** — MVP funcional ponta a ponta + redesenho editorial + tema
claro/escuro e micro-animações.

### v0.1.0 · lançado

- MVP Backend e Frontend (ver checklist abaixo) funcionando ponta a ponta.
- Redesign da UI: linguagem editorial (Fraunces/Inter/Plex Mono), rail
  carbon + conteúdo ivory, hairlines, sem sombras/arredondamentos.
- Tema claro/escuro com acento por modo (sálvia / lilás) e preferência
  do sistema na primeira visita.
- Animações sóbrias (modal com blur da app via portal, transição de
  páginas, stagger do dashboard, press tátil) — respeitando
  `prefers-reduced-motion`.
- READMEs detalhados de backend e frontend (PT + EN).

### v0.1.1 · responsividade mobile

- Navegação por **drawer** no mobile (rail carbon off-canvas, botão `[ menu ]`
  no cabeçalho; fecha ao navegar / `Escape` / clique no overlay).
- Listas de registro viram **cards** em telas < `md` (Clientes, Serviços,
  Estoque, Atendimentos) — tabelas preservadas no desktop.
- Áreas de toque maiores (botões e campos), `PageHeader`, modais e
  cabeçalho/rodapé adaptados por breakpoint.

## Roadmap

### v0.2.0 · CRM (planejado)

- [ ] Perfil completo do cliente (aniversário, origem, preferências, canal) e status (prospect/ativo/inativo)
- [ ] Histórico financeiro do cliente (atendimentos, gasto acumulado, frequência, última visita)
- [ ] Anotações por cliente
- [ ] Aniversariantes e selo de fidelidade (novo/recorrente/frequente)
- [ ] Lembretes de atendimento e follow-up via WhatsApp (`wa.me`)
- [ ] Dashboard: novos vs recorrentes + aniversariantes do mês

### MVP Backend ✅

- [x] Modelagem de entidades (ERD)
- [x] Setup do projeto (Spring Initializr, Postgres)
- [x] Domínio Cliente (entity, repository, service, controller)
- [x] Domínio Serviço
- [x] Domínio Item (estoque)
- [x] Domínio Atendimento
- [x] AtendimentoServico / AtendimentoItem
- [x] Endpoints de leitura (listagem, busca por id, filtros por query param)
- [x] Validações (Bean Validation)
- [x] Tratamento global de exceptions
- [x] CRUD completo (update/delete) e mudança de status do Atendimento
- [x] Testes unitários (59 testes, suite verde)

### MVP Frontend ✅

- [x] Setup Vite + React + TypeScript + Tailwind (palette da marca)
- [x] Layout com sidebar e navegação
- [x] CRUD de Clientes (com busca por nome)
- [x] CRUD de Serviços
- [x] CRUD de Itens + movimentação de estoque
- [x] Atendimentos: filtros, agendamento (serviços + itens extras), mudança de status
- [x] Dashboard (contagens, próximos atendimentos, estoque baixo)

### Deploy

- [ ] Auth/login (usuário master)
- [ ] Escolha de hospedagem gratuita
- [ ] Deploy backend + banco na nuvem
- [ ] PWA / instalação em home screen

## Ideias futuras (fora do escopo do MVP)

- Integração com IA (a definir o caso de uso específico —
  sugestão de horários, previsão de estoque, resumo do dia, etc.)

## Modelagem

![alt text](EDR.png)