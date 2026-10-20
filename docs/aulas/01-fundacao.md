# Aula 01: Fundação

**Objetivo:** ao fim desta aula, você consegue explicar como o Pop in Rio segura ingressos sem nunca vender mais do que existe, e provar isso rodando um teste.

**O que foi construído:** abrir uma venda, segurar ingressos, consultar a venda e a reserva. Com DDD, arquitetura hexagonal, JPA, Flyway, Problem Details, testes de unidade, de concorrência e de arquitetura, Docker e CI.

---

## 1. Mapa do código

Leia nesta ordem. Cada arquivo depende só dos anteriores.

| # | Arquivo | O que olhar |
|---|---|---|
| 1 | `domain/sale/Sale.java` | O agregado e a invariante do estoque |
| 2 | `domain/sale/TicketQuantity.java` | Um objeto de valor que nunca é inválido |
| 3 | `domain/reservation/Reservation.java` | Ciclo de vida, prazo e eventos de domínio |
| 4 | `domain/sale/TicketInventory.java` | A porta que esconde a estratégia de concorrência |
| 5 | `application/ReservationService.java` | O caso de uso e a transação |
| 6 | `adapter/out/persistence/SpringDataSaleRepository.java` | O UPDATE atômico |
| 7 | `adapter/in/web/ApiExceptionHandler.java` | Exceção de domínio virando HTTP |
| 8 | `test/.../ReservationConcurrencyIT.java` | A prova de que funciona |

Todos os caminhos são relativos a `ticketing-service/src/main/java/dev/lucaswotta/popinrio/ticketing/` (ou `src/test/...` para testes).

---

## 2. O caminho de uma requisição

O que acontece quando alguém faz `POST /api/sales/{id}/reservations` com `{"customerId": "lucas", "quantity": 2}`:

1. **`ReservationController`** recebe o JSON. O `@Valid` confere o formato: `customerId` presente, `quantity` presente. Se faltar algo, a resposta é 400 antes de qualquer lógica.
2. **`ReservationService.hold`** abre uma transação (`@Transactional`).
3. Converte os dados crus em objetos de valor: `SaleId`, `CustomerId`, `TicketQuantity`. Confere o limite de 4 por reserva **antes** de tocar no banco.
4. Chama **`TicketInventory.tryWithdraw`**. A implementação atual executa o UPDATE condicional. O PostgreSQL trava a linha da venda, confere se há estoque e subtrai, tudo numa instrução.
5. Se o UPDATE afetou 0 linhas: consulta se a venda existe, para responder 404 ou 409.
6. Se afetou 1 linha: cria a `Reservation` (status `HELD`, prazo de 10 minutos) e grava.
7. Recolhe os eventos da reserva e entrega ao `DomainEventPublisher` (hoje, log).
8. A transação faz commit. Estoque retirado e reserva gravada passam a valer juntos, ou nenhum dos dois.
9. O controller responde 201 com a reserva e o header `Location`.

---

## 3. Conceitos e perguntas de entrevista

Cada conceito tem: o que é, onde está no código e as perguntas que ele responde.

### 3.1 Race condition e overselling

**O que é:** duas transações leem o mesmo estoque (1 ingresso), as duas concluem que há, as duas gravam. Vendeu-se 2 de 1. O padrão perigoso é ler, decidir na aplicação e gravar (*read-modify-write*).

**Onde:** ADR 0003 e `SpringDataSaleRepository.withdrawIfAvailable`.

> **"Como você garante que não vende mais ingresso do que existe?"**
> Com um UPDATE condicional, `where available_tickets >= :quantity`. O banco confere e subtrai numa operação só, segurando o lock da linha. Não há intervalo entre ler e gravar. Além disso, uma CHECK constraint impede estoque negativo, como segunda barreira. E um teste com 500 threads contra 100 ingressos, num PostgreSQL real, prova que saem exatamente 100.

> **"Por que não usou `synchronized` no Java?"**
> Porque só protege dentro de uma JVM. Com duas instâncias da aplicação atrás de um load balancer, cada uma tem o seu `synchronized`, e o overselling volta. A proteção precisa estar no recurso compartilhado, que é o banco.

### 3.2 Agregado, objeto de valor e evento de domínio (DDD)

**O que é:**
- **Agregado:** um grupo de objetos tratado como uma unidade de consistência, com uma raiz que é a única porta de entrada. `Sale` e `Reservation`.
- **Objeto de valor:** definido pelo valor, não por identidade, e imutável. `TicketQuantity`, `CustomerId`.
- **Evento de domínio:** um fato que aconteceu, nomeado no passado. `ReservationHeld`.

**Onde:** `docs/domain.md` e ADR 0002.

> **"Por que a reserva não fica dentro da venda?"**
> Porque um agregado é travado inteiro a cada alteração. Com as reservas dentro da venda, mil compradores disputariam o mesmo objeto gigante. Separados, só o contador de estoque é disputado, e isso o banco resolve com eficiência.

> **"Qual a vantagem de `TicketQuantity` em vez de `int`?"**
> A validação acontece uma vez, na construção. Todo método que recebe um `TicketQuantity` sabe que ele é positivo. Com `int`, cada método teria de conferir, ou confiar que alguém conferiu antes.

> **"Onde você não aplicaria DDD?"**
> No cadastro de usuário, que é CRUD sem regra relevante. DDD tático tem custo de código e só se paga onde há invariantes complexas para proteger.

### 3.3 Arquitetura hexagonal (ports and adapters)

**O que é:** o domínio no centro, sem saber que existe banco ou HTTP. Ele declara **portas** (interfaces, como `TicketInventory`), e a infraestrutura fornece **adaptadores** (implementações, como `AtomicUpdateTicketInventory`).

**Onde:** ADR 0001 e `ArchitectureTest`.

> **"Como você garante que a arquitetura é respeitada?"**
> Com ArchUnit. Há um teste que quebra o build se uma classe do domínio importar Spring ou Hibernate. A regra não depende de disciplina, nem de code review.

> **"Qual o benefício prático?"**
> Na fase 2, vou ter quatro estratégias de concorrência. Todas são adaptadores da mesma porta `TicketInventory`. O caso de uso não muda uma linha.

### 3.4 `@Transactional` e proxies do Spring

**O que é:** o Spring cria um proxy em volta do bean. A chamada passa pelo proxy, que abre a transação, chama o método real e faz commit ou rollback.

**Onde:** `ReservationService.hold` e `AtomicUpdateTicketInventory` (`Propagation.MANDATORY`).

> **"O que acontece se um método `@Transactional` chamar outro `@Transactional` da mesma classe?"**
> A segunda anotação é ignorada. A chamada interna usa `this`, não o proxy, e o proxy é quem aplica a transação. Isso se chama auto-invocação, e é uma das pegadinhas mais comuns do Spring.

> **"Quando acontece rollback?"**
> Por padrão, em exceções não checadas (`RuntimeException`) e `Error`. As exceções de domínio do projeto estendem `RuntimeException`, então um `SoldOutException` desfaz tudo o que a transação fez.

> **"Para que serve `Propagation.MANDATORY`?"**
> Exige uma transação já aberta e falha se não houver. A retirada de estoque só faz sentido junto com a gravação da reserva. Se alguém chamar fora de uma transação, o erro aparece na hora, em vez de virar inconsistência silenciosa.

### 3.5 JPA e Hibernate

**Onde:** `adapter/out/persistence/` e ADR 0004.

> **"Por que separar a entidade JPA do objeto de domínio?"**
> O JPA exige construtor vazio, setters e anotações. Separando, o domínio fica livre do framework. O custo é o mapeamento entre os dois.

> **"O que é `open-in-view` e por que está desligado?"**
> Ligado (o padrão do Spring Boot), mantém a sessão do Hibernate aberta até a resposta HTTP terminar, para permitir carregamento preguiçoso na view. Na prática, segura uma conexão do pool por mais tempo e esconde consultas N+1. Desligado, todo acesso ao banco precisa acontecer dentro da camada de aplicação.

> **"Para que serve `Persistable` nas entidades?"**
> O Spring Data decide entre INSERT e UPDATE olhando se o ID é nulo. Como o ID é gerado pela aplicação, ele nunca é nulo, e o Spring faria um SELECT antes de cada INSERT para descobrir. `Persistable` diz explicitamente se a entidade é nova.

> **"O que é o problema N+1?"**
> Carregar uma lista de N itens e depois disparar mais uma consulta para cada um, por causa de um relacionamento preguiçoso. Aqui ele é evitado por desenho: reservas guardam só o ID da venda, sem `@ManyToOne`.

### 3.6 Flyway

**O que é:** migrações de banco versionadas (`V1__...sql`), aplicadas em ordem e registradas numa tabela de controle.

**Onde:** `src/main/resources/db/migration/`.

> **"Por que não deixar o Hibernate criar as tabelas?"**
> `ddl-auto: update` não versiona nada, não sabe renomear coluna e pode perder dados. Com Flyway, o schema é código revisado, e o Hibernate só valida que as entidades batem com ele.

> **"Você altera uma migração que já rodou?"**
> Nunca. O Flyway guarda o checksum e falha se o arquivo mudar. Correção é sempre uma migração nova.

### 3.7 Validação: 400 ou 422?

**Onde:** `HoldTicketsRequest` (formato) e `Reservation.requireAllowedQuantity` (regra).

> **"Onde fica a validação?"**
> Em dois níveis, com papéis diferentes. Bean Validation, na borda, confere o formato: campo presente, tamanho máximo. Resposta 400. O domínio confere as regras de negócio: no máximo 4 ingressos por reserva. Resposta 422. A regra de negócio mora num lugar só, e a borda não precisa conhecê-la.

### 3.8 Problem Details (RFC 9457)

**Onde:** `ApiExceptionHandler`.

> **"Como sua API comunica erros?"**
> No formato padrão `application/problem+json`, com `status`, `title` e `detail`, mais um campo `code` estável (`SOLD_OUT`, `NOT_FOUND`). O front-end decide o que mostrar pelo `code`, sem depender do texto da mensagem.

### 3.9 Testes

| Tipo | Arquivo | Prova |
|---|---|---|
| Unidade (domínio) | `SaleTest`, `ReservationTest` | As regras de negócio, em milissegundos |
| Unidade (caso de uso) | `ReservationServiceTest` | A orquestração, com portas em memória e sem Spring |
| Arquitetura | `ArchitectureTest` | As dependências entre camadas |
| Integração (API) | `TicketingApiIT` | HTTP de verdade, do JSON ao Problem Details |
| Concorrência | `ReservationConcurrencyIT` | 500 compradores, 100 ingressos, nunca 101 |

> **"Por que Testcontainers e não H2?"**
> Porque o que está sendo testado é o comportamento de lock do PostgreSQL. Um banco em memória diferente pode passar no teste e falhar em produção.

> **"Como você testa concorrência?"**
> Preparo todas as threads e solto todas juntas com um `CountDownLatch`, para maximizar a disputa pela mesma linha. Uso virtual threads, que deixam criar centenas sem custo. Depois confiro três coisas: quantas reservas deram certo, quanto sobrou no estoque e se estoque + reservado = total.

> **"Por que `*Test` e `*IT`?"**
> Os `*Test` rodam no `mvn test` pelo Surefire, sem Docker. Os `*IT` rodam no `mvn verify` pelo Failsafe, com Testcontainers. O ciclo rápido de desenvolvimento não espera o banco subir.

### 3.10 Virtual threads (Java 21)

**Onde:** `spring.threads.virtual.enabled: true` no `application.yml` e no teste de concorrência.

> **"O que são virtual threads?"**
> Threads leves, gerenciadas pela JVM em vez do sistema operacional. Quando uma delas espera I/O (banco, rede), libera a thread do sistema para outra. Uma aplicação que passa a maior parte do tempo esperando o banco atende muito mais requisições simultâneas com o mesmo hardware.

> **"Elas resolvem o gargalo do banco?"**
> Não. O pool de conexões continua com 20 conexões. Virtual threads deixam a aplicação aceitar mais requisições, mas elas esperam na fila do pool. O limite real continua sendo o banco.

### 3.11 Docker

**Onde:** `Dockerfile`, `docker-compose.yml`.

> **"Por que dois estágios no Dockerfile?"**
> O primeiro tem o JDK e o Maven para compilar. O segundo tem só o JRE e o resultado. A imagem final fica menor e não carrega compilador nem código-fonte, o que também reduz a superfície de ataque.

> **"Por que extrair o jar em camadas?"**
> Dependências mudam pouco e o código muda a cada commit. Em camadas separadas, um deploy novo só transfere a camada do código, de alguns KB, em vez do jar inteiro.

> **"Por que o container não roda como root?"**
> Se alguém explorar uma falha na aplicação, fica preso a um usuário sem privilégios dentro do container.

---

## 4. Como rodar

**Pré-requisitos:** Docker e Java 21.

```bash
# Tudo em containers
docker compose up --build

# Abrir uma venda
curl -i -X POST localhost:8080/api/sales \
  -H 'Content-Type: application/json' \
  -d '{"name": "Pop in Rio - Noite 1", "totalTickets": 100}'

# Segurar ingressos (troque SALE_ID pelo id retornado acima)
curl -i -X POST localhost:8080/api/sales/SALE_ID/reservations \
  -H 'Content-Type: application/json' \
  -d '{"customerId": "lucas", "quantity": 2}'

# Todos os testes, inclusive o de concorrência (precisa do Docker rodando)
./mvnw verify

# Só os testes rápidos, sem Docker
./mvnw test
```

---

## 5. Exercício de dono

Faça antes de seguir para a aula 02. Se travar, me chame.

1. **Quebre de propósito.** Em `SpringDataSaleRepository`, apague a linha `and s.availableTickets >= :quantity`. Rode `./mvnw verify`. Observe qual teste falha e qual erro aparece. Dica: a CHECK constraint do banco entra em ação. Depois, desfaça a mudança.
2. **Mude uma regra.** O limite passou de 4 para 6 ingressos por reserva. Descubra todos os lugares que precisam mudar, inclusive uma migração nova (a V1 não pode ser alterada). Rode os testes.
3. **Explique em voz alta, sem olhar:** o caminho de uma requisição (seção 2) e por que `Sale` e `Reservation` são agregados separados.

---

## Próxima aula

**Aula 02: Comparação de estratégias.** Implementar a estratégia ingênua (e ver o overselling acontecer), o lock pessimista e o lock otimista, medir as quatro com k6 e escrever o ADR com os números.
