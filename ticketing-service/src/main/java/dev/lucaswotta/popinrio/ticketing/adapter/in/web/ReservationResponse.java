package dev.lucaswotta.popinrio.ticketing.adapter.in.web;

import dev.lucaswotta.popinrio.ticketing.domain.reservation.Reservation;
import dev.lucaswotta.popinrio.ticketing.domain.reservation.ReservationStatus;

import java.time.Instant;
import java.util.UUID;

record ReservationResponse(
        UUID id,
        UUID saleId,
        String customerId,
        int quantity,
        ReservationStatus status,
        Instant createdAt,
        Instant expiresAt) {

    static ReservationResponse from(Reservation reservation) {
        return new ReservationResponse(
                reservation.getId().value(),
                reservation.getSaleId().value(),
                reservation.getCustomerId().value(),
                reservation.getQuantity().value(),
                reservation.getStatus(),
                reservation.getCreatedAt(),
                reservation.getExpiresAt());
    }
}
