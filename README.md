# Cliniva

Sistema de gestão para clínica de estética de pequeno porte —
agenda, clientes, serviços, controle de estoque e financeiro.

## Contexto

Desenvolvido para resolver um problema real (sistema de gestão
acessível pra pequenos negócios) e como projeto de evolução técnica
em backend (Java/Spring Boot) e frontend (React).

## Stack

- **Backend:** Java, Spring Boot, PostgreSQL, Maven
- **Frontend:** React (planejado)
- **Deploy:** Cloud gratuita (a definir — Render/Railway/Fly.io)

## Status

🚧 Em desenvolvimento — `MVP Backend em andamento`

## Roadmap

### MVP Backend

- [x] Modelagem de entidades (ERD)
- [x] Setup do projeto (Spring Initializr, Postgres)
- [x] Domínio Cliente (entity, repository, service, controller)
- [ ] Domínio Serviço
- [ ] Domínio Item (estoque)
- [ ] Domínio Atendimento
- [ ] AtendimentoServico / AtendimentoItem
- [ ] Validações (Bean Validation)
- [ ] Tratamento global de exceptions
- [ ] Testes unitários

### MVP Frontend

- [ ] Estudo de React aplicado ao projeto
- [ ] Telas de CRUD (Cliente, Serviço, Item)
- [ ] Tela de Atendimento
- [ ] Dashboard de status

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
