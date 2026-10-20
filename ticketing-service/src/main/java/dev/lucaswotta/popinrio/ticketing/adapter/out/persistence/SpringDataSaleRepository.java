package dev.lucaswotta.popinrio.ticketing.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

interface SpringDataSaleRepository extends JpaRepository<SaleJpaEntity, UUID> {

    /**
     * Confere e retira o estoque numa única instrução. O PostgreSQL trava a linha durante o UPDATE,
     * então duas transações nunca leem o mesmo saldo e decidem as duas que há estoque.
     *
     * <p>{@code clearAutomatically} limpa o contexto de persistência depois do UPDATE: sem isso,
     * uma {@code SaleJpaEntity} já carregada na mesma transação continuaria mostrando o saldo
     * antigo.
     *
     * @return 1 se retirou, 0 se a venda não existe ou não tem estoque suficiente
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(
            """
            update SaleJpaEntity s
               set s.availableTickets = s.availableTickets - :quantity
             where s.id = :id
               and s.availableTickets >= :quantity
            """)
    int withdrawIfAvailable(@Param("id") UUID id, @Param("quantity") int quantity);
}
