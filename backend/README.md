# Clíniva — Backend API

API REST do sistema de gestão Clíniva. Gerencia **clientes**, **serviços**,
**itens/estoque** e **atendimentos** (agenda + serviços + itens consumidos).

> Documentação técnica em inglês: [`README.en.md`](./README.en.md)

## Stack

| Camada | Tecnologia |
|--------|------------|
| Linguagem | Java 21 |
| Framework | Spring Boot 4.1 |
| Persistência | Spring Data JPA + Hibernate, PostgreSQL |
| Build | Maven 3.9+ |
| Validação | Bean Validation (`jakarta.validation`) |
| Testes | JUnit 5 + Mockito (H2 em memória) |
| Extras | Lombok, spring-boot-devtools |

## Estrutura

Código organizado **por domínio** (agregado), não por camada técnica:

```
src/main/java/com/cliniva/
├── ClinivaApplication.java       Bootstrap do Spring Boot
├── cliente/                      Domínio Cliente
│   ├── Cliente.java              Entidade JPA
│   ├── ClienteRepository.java    Interface Spring Data
│   ├── ClienteService.java       Regras de negócio
│   ├── ClienteController.java    Endpoints REST
│   └── dtos/                     Records de entrada/saída
├── servico/                      Domínio Serviço (catálogo)
├── item/                         Domínio Item (estoque)
├── atendimento/                  Domínio Atendimento (agenda)
│   ├── enums/StatusAtendimento.java
│   └── model/                    Atendimento, AtendimentoServico, AtendimentoItem
└── exception/                    Tratamento global de erros + exceptions de domínio
```

### Convenções

- **Pacotes por domínio**: cada domínio contém `Entity`, `Repository`,
  `Service`, `Controller` e seus `dtos/`.
- **DTOs em `record`**: de entrada (`Create*/Update*RequestDTO`) e de saída
  (`*ResponseDTO`), separando o contrato externo das entidades.
- **IDs `UUID`**, gerados pela aplicação.
- **Decimais** para valores monetários e quantidades (`BigDecimal`).

## Pré-requisitos

- Java 21
- Maven 3.9+
- PostgreSQL rodando em `localhost:5432`

## Banco de dados

Crie o banco e o usuário uma única vez:

```bash
sudo -u postgres psql -c "CREATE ROLE cliniva LOGIN PASSWORD 'cliniva';"
sudo -u postgres psql -c "CREATE DATABASE cliniva OWNER cliniva;"
```

As tabelas são criadas/atualizadas automaticamente pelo Hibernate
(`spring.jpa.hibernate.ddl-auto=update`) — o `CREATE ROLE`/`CREATE DATABASE`
é só a primeira vez.

### Configuração (variáveis de ambiente)

| Variável | Padrão |
|----------|--------|
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/cliniva` |
| `SPRING_DATASOURCE_USER` | `cliniva` |
| `SPRING_DATASOURCE_PASSWORD` | `cliniva` |

## Rodar

```bash
cd backend
mvn spring-boot:run          # API em http://localhost:8080
```

Testes (H2 em memória, não precisa de banco):

```bash
mvn test                     # 59 testes unitários
```

Smoke test:

```bash
curl http://localhost:8080/api/clientes
```

## API — referência

Todas as rotas têm o prefixo `/api`. Corpos de entrada/saída em JSON.

### Clientes — `/api/clientes`

| Método | Rota | Descrição |
|--------|------|-----------|
| `GET` | `/api/clientes` | Lista (filtro opcional `?nome=`) |
| `GET` | `/api/clientes/{id}` | Busca por id |
| `POST` | `/api/clientes` | Cria |
| `PUT` | `/api/clientes/{id}` | Atualiza |
| `DELETE` | `/api/clientes/{id}` | Remove (409 se tiver atendimentos) |

**`POST` / `PUT` — corpo:**

```json
{
  "nome": "Maria Silva",
  "email": "maria@example.com",
  "telefone": "(11) 91234-5678"
}
```

Validações: `nome` e `telefone` obrigatórios (máx. 120 e 20 caracteres);
`email` válido se presente (máx. 120).

**Resposta (`GET /{id}` e listagem):**

```json
{ "id": "0b6f...", "nome": "Maria Silva", "email": "maria@example.com", "telefone": "(11) 91234-5678" }
```

### Serviços — `/api/servicos`

| Método | Rota | Descrição |
|--------|------|-----------|
| `GET` | `/api/servicos` | Lista |
| `GET` | `/api/servicos/{id}` | Busca por id |
| `POST` | `/api/servicos` | Cria |
| `PUT` | `/api/servicos/{id}` | Atualiza |
| `DELETE` | `/api/servicos/{id}` | Remove (409 se em uso) |

**`POST` / `PUT` — corpo:**

```json
{
  "nome": "Limpeza de Pele",
  "descricao": "Limpeza profunda com extração",
  "valor": 150.00
}
```

Validações: `nome` obrigatório (máx. 120); `descricao` máx. 500; `valor`
obrigatório e positivo.

### Itens (estoque) — `/api/items`

| Método | Rota | Descrição |
|--------|------|-----------|
| `GET` | `/api/items` | Lista |
| `GET` | `/api/items/{id}` | Busca por id |
| `POST` | `/api/items` | Cria |
| `PUT` | `/api/items/{id}` | Atualiza |
| `DELETE` | `/api/items/{id}` | Remove (409 se em uso) |
| `PATCH` | `/api/items/{id}/estoque` | Movimenta estoque |

**`POST` / `PUT` — corpo:**

```json
{ "nome": "Ácido Hialurônico", "quantidadeEmEstoque": 10 }
```

**`PATCH .../estoque` — corpo:**

```json
{ "tipo": "ENTRADA", "quantidade": 5 }
```

`tipo`: `ENTRADA` ou `SAIDA`. Saída maior que o saldo → **409**
(`EstoqueInsuficienteException`); quantidades negativas → **400**.

### Atendimentos — `/api/atendimentos`

| Método | Rota | Descrição |
|--------|------|-----------|
| `GET` | `/api/atendimentos` | Lista com filtros |
| `GET` | `/api/atendimentos/{id}` | Detalhe completo |
| `POST` | `/api/atendimentos` | Agenda (consome estoque) |
| `PUT` | `/api/atendimentos/{id}` | Atualiza (só se `AGENDADO`) |
| `PATCH` | `/api/atendimentos/{id}/status` | Muda status |

**`GET` — filtros** (todos opcionais):

```
?status=AGENDADO        filtra por status
?clienteId=<uuid>       filtra por cliente
?dataInicio=<ISO>       data mínima (LocalDateTime)
?dataFim=<ISO>          data máxima (LocalDateTime)
```

**`POST` — corpo:**

```json
{
  "clienteId": "0b6f...",
  "dataAtendimento": "2026-09-06T14:30:00",
  "servicos": [
    {
      "servicoId": "3f2a...",
      "itensExtras": [ { "itemId": "9c11...", "quantidade": 2 } ]
    }
  ]
}
```

Criar um atendimento **abate o estoque** automaticamente (itens do serviço +
extras); falta de estoque → **409**.

**`PATCH .../{id}/status` — corpo:**

```json
{ "novoStatus": "CONCLUIDO" }
```

**Resposta (`GET /{id}`):**

```json
{
  "id": "a12c...",
  "clienteId": "0b6f...",
  "nomeCliente": "Maria Silva",
  "dataAtendimento": "2026-09-06T14:30:00",
  "dataCriacao": "2026-09-06",
  "status": "AGENDADO",
  "valorTotal": 170.00,
  "servicos": [
    {
      "servicoId": "3f2a...",
      "nomeServico": "Limpeza de Pele",
      "valorCobrado": 150.00,
      "itensUsados": [ { "itemId": "9c11...", "nomeItem": "Ácido Hialurônico", "quantidadeUsada": 2 } ]
    }
  ]
}
```

A listagem retorna a versão resumo: `id`, `nomeCliente`, `dataAtendimento`,
`status`, `valorTotal`.

### Regras de domínio do status

```
AGENDADO ──► CONCLUIDO
AGENDADO ──► CANCELADO   (devolve o estoque abatido)
CONCLUIDO ──► CANCELADO  (correção de digitação; NÃO devolve estoque)
CANCELADO ──► (estado terminal, não muda mais)
```

- Mudar para o status atual → **409**.
- `PUT /{id}` só é permitido para atendimentos `AGENDADO` → senão **409**.

## Tratamento de erros

Todas as falhas retornam o formato:

```json
{
  "status": 404,
  "mensagem": "Recurso não encontrado",
  "erros": []
}
```

| HTTP | Causa |
|------|-------|
| `400` | Validação de campos, JSON malformado, tipo inválido em query param |
| `404` | Recurso inexistente |
| `409` | Conflito: duplicado, estoque insuficiente, transição de status inválida, recurso em uso |

Exceções mapeadas no [`GlobalExceptionHandler`](src/main/java/com/cliniva/exception/GlobalExceptionHandler.java).

## Modelagem

O diagrama entidade-relacionamento fica na raiz do repositório: [`EDR.png`](../EDR.png).