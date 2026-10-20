package dev.lucaswotta.popinrio.ticketing.domain.sale;

import dev.lucaswotta.popinrio.ticketing.domain.shared.DomainException;

public class SaleNotFoundException extends DomainException {

    public SaleNotFoundException(SaleId saleId) {
        super("Venda não encontrada: " + saleId);
    }
}
