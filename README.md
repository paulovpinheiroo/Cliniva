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

Executar os testes (79 unit/integration tests, usa H2 em memória — não precisa de banco):

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
| Clientes | `GET/POST /api/clientes` · `GET/PUT/DELETE /api/clientes/{id}` · busca por `?nome=`, `?status=` |
| Cliente · CRM | `GET /api/clientes/{id}/historico` · `GET/POST /api/clientes/{id}/notas` · `DELETE /api/clientes/{id}/notas/{notaId}` · `GET /api/clientes/aniversariantes?mes=` |
| Serviços | `GET/POST /api/servicos` · `GET/PUT/DELETE /api/servicos/{id}` |
| Itens/Estoque | `GET/POST /api/items` · `GET/PUT/DELETE /api/items/{id}` · `PATCH /api/items/{id}/estoque` |
| Atendimentos | `GET/POST /api/atendimentos` · `GET /api/atendimentos/{id}` · `PATCH /{id}/status` |

Filtros de atendimento: `?status=`, `?clienteId=`, `?dataInicio=`, `?dataFim=`.

Erros seguem o formato `{"status", "mensagem", "erros"}` (400/404/409).

## Status

🚀 **v0.2.0** — CRM de clientes: perfil completo, status, histórico
financeiro, anotações, aniversariantes, fidelização e WhatsApp.

### v0.2.0 · CRM

- Perfil completo do cliente: data de nascimento, origem
  (indicação/Instagram/Google/passou na rua), canal preferido,
  preferências e observações; status prospect/ativo/inativo.
- **Histórico financeiro** por cliente: atendimentos, gasto acumulado,
  ticket médio, última visita e frequência (`GET /clientes/{id}/historico`).
- **Anotações** por cliente (listar/adicionar/excluir) e bloqueio de
  exclusão de cliente com vínculos.
- **Aniversariantes do mês** (`GET /clientes/aniversariantes?mes=`) e
  selo de fidelidade na lista (novo / recorrente / frequente).
- **WhatsApp**: follow-up na ficha do cliente e lembretes de
  atendimento agendado (link `wa.me` pré-preenchido, sem custo).
- Página **/clientes/:id** com ficha completa, financeiro, histórico e
  anotações (responsiva).
- Dashboard: **novos vs recorrentes** no mês e aniversariantes (com
  botão de saudação via WhatsApp).
- 79 testes backend verdes (unidade + integração de repositório).

### v0.1.1 · responsividade mobile

- Navegação por **drawer** no mobile (rail carbon off-canvas, botão `[ menu ]`
  no cabeçalho; fecha ao navegar / `Escape` / clique no overlay).
- Listas de registro viram **cards** em telas < `md` (Clientes, Serviços,
  Estoque, Atendimentos) — tabelas preservadas no desktop.
- Áreas de toque maiores (botões e campos), `PageHeader`, modais e
  cabeçalho/rodapé adaptados por breakpoint.

## Roadmap

### v0.3.0 · Deploy & Auth (planejado)

- [ ] Autenticação/login de usuário (master)
- [ ] Escolha de hospedagem gratuita e deploy do backend + banco
- [ ] Deploy do frontend (Vercel/Netlify) e PWA / home screen

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
- [x] Testes unitários (79 testes, suite verde)

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