package dev.lucaswotta.popinrio.ticketing.domain.reservation;

import dev.lucaswotta.popinrio.ticketing.domain.shared.DomainException;

public class ReservationNotFoundException extends DomainException {

    public ReservationNotFoundException(ReservationId id) {
        super("Reserva não encontrada: " + id);
    }
}
