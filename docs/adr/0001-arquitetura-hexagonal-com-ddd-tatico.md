# ADR 0001: Arquitetura hexagonal com DDD tático no contexto de Ingressos

- **Status:** aceito
- **Data:** 2026-10-20

## Contexto

O núcleo do sistema é uma regra de negócio com consequência financeira: nunca vender mais ingressos do que existem. Essa regra precisa ser testável sem banco, sem servidor web e sem framework, e não pode se espalhar por controllers e repositórios.

Ao mesmo tempo, o projeto vai trocar peças de infraestrutura ao longo das fases: estratégias de concorrência diferentes, publicação de eventos por log e depois por outbox e Kafka, estoque em PostgreSQL e depois em Redis.

## Decisão

Organizar o `ticketing-service` em três camadas, com dependências apontando sempre para dentro:

- **`domain`**: agregados, objetos de valor, eventos e interfaces de repositório. Java puro.
- **`application`**: casos de uso. Orquestram o domínio e definem a transação.
- **`adapter`**: tudo que fala com o mundo externo. `in.web` (REST) e `out.persistence` (JPA).

Regras verificadas por teste ([`ArchitectureTest`](../../ticketing-service/src/test/java/dev/lucaswotta/popinrio/ticketing/architecture/ArchitectureTest.java)), que quebram o build se forem violadas:

1. O domínio não importa Spring, Jakarta, Hibernate nem Jackson.
2. O domínio não conhece `application`, `adapter` nem `config`.
3. A camada de aplicação não conhece adaptadores.

DDD tático (agregados, objetos de valor, eventos) é aplicado só no contexto de Ingressos. O contexto de Identidade, quando existir, será CRUD simples.

## Consequências

- **Bom:** o domínio é testado em milissegundos, sem subir o Spring. Trocar a estratégia de estoque é trocar um adaptador.
- **Bom:** a regra fica num lugar só. Para saber o que é uma reserva válida, lê-se `Reservation.java`.
- **Custo:** existe duplicação entre o agregado e a entidade JPA, além dos mapeamentos entre os dois. É o preço de o domínio não depender do framework de persistência (ver ADR 0004).
- **Custo:** mais arquivos e mais indireção do que um CRUD em camadas. Para um CRUD, essa arquitetura seria excessiva; aqui, a complexidade da regra justifica.
