package dev.lucaswotta.popinrio.ticketing.domain.reservation;

import dev.lucaswotta.popinrio.ticketing.domain.shared.DomainException;

public class InvalidReservationTransitionException extends DomainException {

    public InvalidReservationTransitionException(
            ReservationId id, ReservationStatus from, ReservationStatus to, String reason) {
        super("Reserva %s não pode ir de %s para %s: %s".formatted(id, from, to, reason));
    }
}
