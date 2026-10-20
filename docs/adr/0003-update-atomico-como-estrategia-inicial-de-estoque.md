# ADR 0003: UPDATE atômico condicional como estratégia inicial de estoque

- **Status:** aceito (será comparado com outras estratégias na fase 2)
- **Data:** 2026-10-20

## Contexto

O jeito ingênuo de retirar do estoque é: ler a venda, conferir se há ingressos, subtrair e gravar. Sob concorrência, duas transações leem o mesmo saldo (digamos, 1 ingresso), as duas concluem que há estoque, e as duas gravam. Vendeu-se 2 de 1. Esse padrão se chama *read-modify-write* e é a causa clássica de overselling.

As alternativas corretas mais comuns:

| Estratégia | Como evita o problema | Custo |
|---|---|---|
| Lock pessimista (`SELECT ... FOR UPDATE`) | Trava a linha na leitura; os outros esperam | Todos os compradores fazem fila atrás de uma transação que lê, pensa e grava |
| Lock otimista (`@Version`) | Grava só se ninguém alterou desde a leitura; senão, tenta de novo | Com milhares disputando a mesma linha, quase toda tentativa colide e é refeita |
| UPDATE atômico condicional | Confere e altera numa instrução só | A regra "há estoque?" fica no SQL, fora do método do agregado |

## Decisão

Começar com o UPDATE atômico condicional:

```sql
update sale
   set available_tickets = available_tickets - :quantity
 where id = :id
   and available_tickets >= :quantity
```

O banco avalia a condição e aplica a subtração enquanto segura o lock da linha. Não existe intervalo entre "ler" e "gravar" em que outra transação possa se meter. O número de linhas afetadas (1 ou 0) diz se a retirada aconteceu.

A operação exige uma transação já aberta (`Propagation.MANDATORY`), porque ela só faz sentido junto com a gravação da reserva.

## Consequências

- **Bom:** uma ida ao banco por tentativa, o lock é segurado pelo menor tempo possível, e não há retry.
- **Bom:** a CHECK constraint `available_tickets between 0 and total_tickets` continua lá como segunda barreira.
- **Custo:** a regra de negócio aparece em dois lugares, em `Sale.withdraw` e no SQL. É uma duplicação consciente, coberta por teste nos dois lados.
- **Custo:** ainda há uma linha quente (todos disputam a mesma linha). Se isso virar gargalo, as saídas são fragmentar o contador em várias linhas ou mover o estoque para Redis. Ambas ficam para fases posteriores, e só com medição mostrando a necessidade.
- **A ser feito:** na fase 2, implementar as outras estratégias (inclusive a ingênua, para demonstrar o overselling) e decidir com números do k6.
