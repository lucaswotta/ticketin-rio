# Modelo de domínio

Este documento registra como o Pop in Rio foi modelado com Domain-Driven Design: os contextos, os agregados, a linguagem e, tão importante quanto, onde DDD **não** foi aplicado.

## O negócio em uma frase

Um show tem um número fixo de ingressos. Quando a venda abre, milhares de pessoas tentam comprar ao mesmo tempo. Cada comprador segura alguns ingressos por um prazo curto; se não pagar dentro do prazo, os ingressos voltam para a venda. Nunca se vende mais ingresso do que existe.

## Linguagem ubíqua

O código está em inglês, como é padrão no mercado. Este glossário é o contrato entre o termo do negócio e o nome no código: se alguém do negócio diz "segurar ingresso", todo mundo sabe que é `hold`.

| Negócio (PT) | Código (EN) | Significado |
|---|---|---|
| Venda | `Sale` | Um show com um estoque fixo de ingressos à venda |
| Estoque disponível | `availableTickets` | Ingressos que ainda podem ser segurados |
| Segurar ingressos | `hold` | Retirar ingressos do estoque e reservá-los para um comprador, com prazo |
| Reserva | `Reservation` | Ingressos segurados para um comprador |
| Prazo da reserva | `expiresAt` | Instante a partir do qual a reserva não pode mais ser confirmada |
| Confirmar | `confirm` | O pagamento foi aprovado; os ingressos são do comprador |
| Expirar | `expire` | O prazo venceu sem pagamento; os ingressos voltam ao estoque |
| Cancelar | `cancel` | O comprador desistiu antes do prazo; os ingressos voltam ao estoque |
| Esgotado | `SoldOut` | Não há estoque suficiente para o pedido |
| Comprador | `Customer` | Quem compra. No contexto de Ingressos, só uma referência (`CustomerId`) |

## Contextos delimitados

```
                 ┌─────────────────────────┐
                 │       Identidade        │  cadastro e login (CRUD, sem DDD tático)
                 └────────────┬────────────┘
                              │ CustomerId (só a referência atravessa)
                              ▼
┌──────────────────────────────────────────┐   eventos    ┌──────────────────────────┐
│                Ingressos                 │ ───────────► │        Pagamentos        │
│   Sale (estoque) · Reservation (prazo)   │ ◄─────────── │   payment-service        │
│   ticketing-service                      │   eventos    │   (fase de mensageria)   │
└──────────────────────────────────────────┘              └────────────┬─────────────┘
                                                                       │ camada anticorrupção
                                                                       ▼
                                                           Gateway de pagamento externo
                                                           (simulado, instável de propósito)
```

**Ingressos** é o núcleo do sistema, onde mora o problema difícil (concorrência sobre o estoque). É aqui que o DDD tático é aplicado com rigor.

**Pagamentos** é um contexto separado, num serviço separado. Ingressos e Pagamentos só conversam por eventos: Ingressos nunca chama Pagamentos diretamente, e vice-versa. O gateway externo fala uma linguagem própria (códigos de retorno, formatos), e uma camada anticorrupção traduz isso para o modelo de Pagamentos, para que a bagunça do mundo externo não vaze para dentro.

**Identidade** é um CRUD de cadastro e login. Não tem invariante interessante para proteger, então não ganha agregado, objeto de valor nem evento de domínio. Forçar DDD tático aqui seria cerimônia sem benefício.

### Por que "Vendas" e "Reservas" não são contextos separados

Na primeira versão do desenho eles eram candidatos a contextos distintos. Foram unidos em **Ingressos** porque:

1. Falam a mesma língua: "ingresso", "estoque" e "reserva" significam a mesma coisa para os dois.
2. A regra mais importante do sistema, "retirar do estoque e criar a reserva", precisa acontecer **na mesma transação**. Separar em contextos obrigaria a coordenar essa operação por eventos (consistência eventual) exatamente no ponto onde a consistência imediata é o requisito.

A fronteira de um contexto é uma fronteira de consistência e de linguagem, não de "assuntos diferentes".

## Agregados

### `Sale`

- **Protege:** `0 <= availableTickets <= totalTickets`.
- **Operação central:** `withdraw(TicketQuantity)`.
- **Reforço no banco:** CHECK constraint com a mesma regra, como última linha de defesa.

### `Reservation`

- **Protege:** o ciclo de vida `HELD → CONFIRMED | EXPIRED | CANCELLED` e o prazo. Uma reserva vencida não pode ser confirmada, mesmo que o job de expiração ainda não tenha passado por ela.
- **Referencia a venda só por ID** (`SaleId`), nunca pelo objeto.
- **Emite eventos:** `ReservationHeld`, `ReservationConfirmed`, `ReservationExpired`, `ReservationCancelled`.

Por que são dois agregados, e não "uma venda com uma lista de reservas", está no [ADR 0002](adr/0002-sale-e-reservation-como-agregados-separados.md).

## Objetos de valor

| Objeto | Garante |
|---|---|
| `SaleId`, `ReservationId` | Identidade tipada: impossível passar um ID de reserva onde se espera um de venda |
| `TicketQuantity` | Sempre positiva depois de construída |
| `CustomerId` | Nunca vazio, no máximo 60 caracteres, sem espaços nas pontas |

O ganho é que nenhum método que recebe um `TicketQuantity` precisa perguntar "e se for zero?". A validação acontece uma vez, na construção.

## Eventos de domínio

Os agregados registram eventos quando algo relevante acontece. A camada de aplicação recolhe esses eventos depois de salvar o agregado e entrega para a porta `DomainEventPublisher`.

Hoje, a implementação só registra no log. Na fase de mensageria, ela passa a gravar na tabela de outbox, na mesma transação, e um relay publica no Kafka. O caso de uso não muda nenhuma linha nessa troca.
