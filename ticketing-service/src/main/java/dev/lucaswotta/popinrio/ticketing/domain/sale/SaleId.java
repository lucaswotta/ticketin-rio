package dev.lucaswotta.popinrio.ticketing.domain.sale;

import java.util.Objects;
import java.util.UUID;

public record SaleId(UUID value) {

    public SaleId {
        Objects.requireNonNull(value, "SaleId não pode ser nulo");
    }

    public static SaleId newId() {
        return new SaleId(UUID.randomUUID());
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
