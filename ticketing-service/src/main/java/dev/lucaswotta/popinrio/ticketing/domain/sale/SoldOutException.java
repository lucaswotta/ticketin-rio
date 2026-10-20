package dev.lucaswotta.popinrio.ticketing.domain.sale;

import dev.lucaswotta.popinrio.ticketing.domain.shared.DomainException;

public class SoldOutException extends DomainException {

    public SoldOutException(SaleId saleId, TicketQuantity requested) {
        super(
                "Ingressos insuficientes na venda "
                        + saleId
                        + " para "
                        + requested.value()
                        + " unidade(s)");
    }
}
