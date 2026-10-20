package dev.lucaswotta.popinrio.ticketing.domain.reservation;

import dev.lucaswotta.popinrio.ticketing.domain.sale.SaleId;
import dev.lucaswotta.popinrio.ticketing.domain.shared.DomainEvent;

import java.time.Instant;

/** Eventos do agregado Reservation. */
public final class ReservationEvents {

    private ReservationEvents() {}

    public record ReservationHeld(
            ReservationId reservationId,
            SaleId saleId,
            CustomerId customerId,
            int quantity,
            Instant expiresAt,
            Instant occurredAt)
            implements DomainEvent {}

    public record ReservationConfirmed(
            ReservationId reservationId, SaleId saleId, int quantity, Instant occurredAt)
            implements DomainEvent {}

    /** Os ingressos precisam voltar ao estoque. */
    public record ReservationExpired(
            ReservationId reservationId, SaleId saleId, int quantity, Instant occurredAt)
            implements DomainEvent {}

    /** Os ingressos precisam voltar ao estoque. */
    public record ReservationCancelled(
            ReservationId reservationId, SaleId saleId, int quantity, Instant occurredAt)
            implements DomainEvent {}
}
