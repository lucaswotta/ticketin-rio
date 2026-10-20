# ADR 0002: Sale e Reservation como agregados separados

- **Status:** aceito
- **Data:** 2026-10-20

## Contexto

O modelo mais intuitivo seria uma `Sale` contendo a lista das suas `Reservation`s. Toda reserva passaria pela venda, e a venda garantiria sozinha que a soma das reservas não passa do estoque.

O problema aparece sob carga. Um agregado é uma fronteira de consistência: toda alteração nele é feita de forma atômica, travando o agregado inteiro. Se as reservas morassem dentro da venda, cada uma das milhares de requisições simultâneas precisaria carregar e travar a venda com todas as suas reservas.

## Decisão

`Sale` e `Reservation` são agregados separados. A reserva referencia a venda só pelo ID (`SaleId`), sem `@ManyToOne` na persistência.

A única coisa compartilhada e disputada é o contador `availableTickets` da venda. A operação "retirar do estoque e criar a reserva" mantém a consistência por estar numa única transação do caso de uso, que altera os dois agregados.

## Consequências

- **Bom:** a disputa se reduz a um único inteiro numa única linha, que o banco sabe proteger com eficiência (ADR 0003).
- **Bom:** ler ou alterar uma reserva nunca carrega a venda, nem as outras reservas.
- **Custo:** a regra "uma transação altera um agregado só" é quebrada conscientemente no caso de uso de reservar. A alternativa purista seria consistência eventual entre venda e reserva por eventos, o que abriria uma janela em que o estoque já foi retirado sem a reserva existir. Para o problema central do sistema, consistência imediata vale mais que a pureza da regra.
- **Custo:** a invariante "estoque + reservado = total" não é protegida por um agregado só. Ela é provada por teste (`ReservationConcurrencyIT`).
