# ADR 0004: Spring Data JPA com entidades separadas do domínio

- **Status:** aceito
- **Data:** 2026-10-20

## Contexto

A persistência precisa de três coisas ao longo do projeto: o UPDATE atômico (fase 1), lock pessimista e lock otimista (fase 2). O JPA oferece as três de forma idiomática: `@Modifying @Query`, `@Lock(PESSIMISTIC_WRITE)` e `@Version`. Também é a tecnologia de persistência mais cobrada no mercado Java.

O JPA, porém, exige construtor vazio, campos mutáveis e anotações nas classes. Colocar isso nos agregados acoplaria o domínio ao framework, contrariando o ADR 0001.

## Decisão

- Usar Spring Data JPA com Hibernate.
- Manter classes separadas: o agregado (`Sale`) no domínio e a entidade (`SaleJpaEntity`) no adaptador, com conversão explícita entre os dois.
- O schema é do Flyway. O Hibernate roda com `ddl-auto: validate`: confere se as entidades batem com o banco e falha na subida se não baterem.
- `open-in-view: false`, para que a conexão com o banco não fique presa durante a renderização da resposta.
- As entidades implementam `Persistable`, porque os IDs são gerados pela aplicação. Sem isso, o Spring Data trata um ID preenchido como "já existe" e faz um SELECT antes de cada INSERT.

## Consequências

- **Bom:** o domínio continua Java puro. As features de concorrência do JPA ficam disponíveis no adaptador.
- **Custo:** código de mapeamento (`newFrom`, `toDomain`) para cada agregado. Feito à mão, sem MapStruct: são poucos campos, e a conversão explícita é mais fácil de ler e depurar do que código gerado.
- **Custo:** as vantagens de carregamento preguiçoso e dirty checking entre agregados ficam de fora, mas não são necessárias com agregados que se referenciam por ID.
