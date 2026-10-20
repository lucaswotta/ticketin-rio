package dev.lucaswotta.popinrio.ticketing.domain.sale;

import dev.lucaswotta.popinrio.ticketing.domain.shared.DomainException;

public class InvalidSaleException extends DomainException {

    public InvalidSaleException(String message) {
        super(message);
    }
}
