package dev.lucaswotta.popinrio.ticketing.domain.sale;

import dev.lucaswotta.popinrio.ticketing.domain.shared.DomainException;

public class InvalidQuantityException extends DomainException {

    public InvalidQuantityException(String message) {
        super(message);
    }
}
