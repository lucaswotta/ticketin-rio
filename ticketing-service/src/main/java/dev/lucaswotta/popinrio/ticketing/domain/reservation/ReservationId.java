package dev.lucaswotta.popinrio.ticketing.domain.reservation;

import java.util.Objects;
import java.util.UUID;

public record ReservationId(UUID value) {

    public ReservationId {
        Objects.requireNonNull(value, "ReservationId não pode ser nulo");
    }

    public static ReservationId newId() {
        return new ReservationId(UUID.randomUUID());
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
