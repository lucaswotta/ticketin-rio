package dev.lucaswotta.popinrio.ticketing.adapter.out.persistence;

import dev.lucaswotta.popinrio.ticketing.domain.sale.SaleId;
import dev.lucaswotta.popinrio.ticketing.domain.sale.TicketInventory;
import dev.lucaswotta.popinrio.ticketing.domain.sale.TicketQuantity;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Estratégia de UPDATE atômico condicional: a regra "só retira se houver estoque" é avaliada e
 * aplicada pelo próprio banco, numa instrução só. Não há janela entre ler e gravar.
 *
 * <p>{@code MANDATORY}: a retirada só faz sentido dentro da transação do caso de uso, junto com a
 * gravação da reserva. Chamada fora de uma transação é bug, e falha alto em vez de silenciosamente.
 */
@Component
class AtomicUpdateTicketInventory implements TicketInventory {

    private final SpringDataSaleRepository jpa;

    AtomicUpdateTicketInventory(SpringDataSaleRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public boolean tryWithdraw(SaleId saleId, TicketQuantity quantity) {
        return jpa.withdrawIfAvailable(saleId.value(), quantity.value()) == 1;
    }
}
