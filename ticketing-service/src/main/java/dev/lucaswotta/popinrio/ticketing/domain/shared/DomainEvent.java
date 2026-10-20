package dev.lucaswotta.popinrio.ticketing.domain.shared;

import java.time.Instant;

/**
 * Algo relevante que aconteceu no domínio, nomeado no passado (ReservationHeld, não
 * HoldReservation).
 */
public interface DomainEvent {

    Instant occurredAt();
}
