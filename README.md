# Pop in Rio

Venda relâmpago de ingressos sob alta concorrência. Um show, um estoque fixo, milhares de compradores clicando ao mesmo tempo, e uma garantia: **nunca vender mais ingressos do que existem.**

> Projeto de estudo e portfólio. O festival e os ingressos são fictícios. Sem nenhuma afiliação com o Rock in Rio ou qualquer outro evento real.

[![CI](https://github.com/lucaswotta/ticketin-rio/actions/workflows/ci.yml/badge.svg)](https://github.com/lucaswotta/ticketin-rio/actions/workflows/ci.yml)
![Java 21](https://img.shields.io/badge/Java-21-blue)
![Spring Boot 4](https://img.shields.io/badge/Spring%20Boot-4.1-6DB33F)
![PostgreSQL 18](https://img.shields.io/badge/PostgreSQL-18-336791)

## O problema

Quando 100 ingressos entram à venda para milhares de pessoas, o jeito óbvio de vender (ler o estoque, conferir se há, subtrair e gravar) falha: duas requisições leem o mesmo saldo, as duas acham que há ingresso, e as duas vendem. O sistema precisa garantir que:

1. o estoque nunca fica negativo, com qualquer número de requisições simultâneas;
2. cada comprador segura os ingressos por um prazo, e o que não for pago volta ao estoque;
3. a regra continua valendo com várias instâncias da aplicação rodando.

## Como é provado

O teste [`ReservationConcurrencyIT`](ticketing-service/src/test/java/dev/lucaswotta/popinrio/ticketing/application/ReservationConcurrencyIT.java) solta **500 compradores ao mesmo tempo contra 100 ingressos**, num PostgreSQL real via Testcontainers, e confere que saem exatamente 100 reservas, que o estoque termina em zero, e que estoque + reservado = total. Ele roda a cada push no CI.

## Arquitetura

Arquitetura hexagonal com Domain-Driven Design no núcleo:

```
ticketing-service/src/main/java/dev/lucaswotta/popinrio/ticketing
├── domain            Java puro: agregados, objetos de valor, eventos, portas
│   ├── sale          Sale (estoque), TicketQuantity, TicketInventory
│   ├── reservation   Reservation (ciclo de vida e prazo), eventos
│   └── shared        AggregateRoot, DomainEvent, DomainException
├── application       casos de uso e fronteira de transação
├── adapter
│   ├── in.web        REST, validação de formato, Problem Details
│   └── out           JPA (inclusive o UPDATE atômico) e publicação de eventos
└── config
```

Um teste ArchUnit quebra o build se o domínio importar Spring, Jakarta ou Hibernate.

- Modelo de domínio, contextos e glossário: [`docs/domain.md`](docs/domain.md)
- Decisões e trade-offs: [`docs/adr/`](docs/adr/)
- Material de estudo de cada fase: [`docs/aulas/`](docs/aulas/)

## Rodando

Pré-requisitos: Docker e Java 21.

```bash
docker compose up --build          # PostgreSQL + aplicação em http://localhost:8080
./mvnw test                        # testes rápidos (sem Docker)
./mvnw verify                      # todos, inclusive concorrência com Testcontainers
```

```bash
# Abrir uma venda
curl -X POST localhost:8080/api/sales -H 'Content-Type: application/json' \
  -d '{"name": "Pop in Rio - Noite 1", "totalTickets": 100}'

# Segurar ingressos
curl -X POST localhost:8080/api/sales/{saleId}/reservations -H 'Content-Type: application/json' \
  -d '{"customerId": "lucas", "quantity": 2}'
```

| Endpoint | Resposta |
|---|---|
| `POST /api/sales` | 201 com a venda |
| `GET /api/sales/{id}` | 200, ou 404 |
| `POST /api/sales/{id}/reservations` | 201 com a reserva; 409 `SOLD_OUT`; 422 acima de 4 ingressos; 404 venda inexistente |
| `GET /api/reservations/{id}` | 200, ou 404 |

Erros seguem o padrão Problem Details (RFC 9457), com um campo `code` estável.

## Roteiro

- [x] **1. Fundação:** domínio com DDD, UPDATE atômico, JPA, Flyway, testes de concorrência e de arquitetura, Docker, CI
- [ ] **2. Estratégias:** ingênua (demonstra o overselling), lock pessimista e otimista, comparadas com k6
- [ ] **3. Regras completas:** expiração com `SKIP LOCKED`, confirmação, idempotência, limite por comprador
- [ ] **4. Identidade:** cadastro e login com Spring Security e JWT, OpenAPI
- [ ] **5. Mensageria:** outbox transacional, Kafka e o `payment-service` com Resilience4j
- [ ] **6. Redis:** cache, rate limiting e estoque com Lua
- [ ] **7. Observabilidade:** Micrometer, Prometheus, Grafana e tracing com OpenTelemetry
- [ ] **8. Demo ao vivo:** compra à esquerda, raio-x do backend em tempo real à direita, 1.000 compradores simulados
- [ ] **9. Deploy:** Oracle Cloud Always Free, Caddy com HTTPS, deploy contínuo pelo GitHub Actions

## Stack

Java 21 · Spring Boot 4.1 · Spring Data JPA / Hibernate · PostgreSQL 18 · Flyway · JUnit · AssertJ · Testcontainers · ArchUnit · Spotless · Docker · GitHub Actions
