package dev.lucaswotta.popinrio.ticketing.adapter.in.web;

import dev.lucaswotta.popinrio.ticketing.application.HoldTicketsCommand;
import dev.lucaswotta.popinrio.ticketing.application.ReservationService;
import dev.lucaswotta.popinrio.ticketing.domain.reservation.Reservation;
import dev.lucaswotta.popinrio.ticketing.domain.reservation.ReservationId;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

@RestController
class ReservationController {

    private final ReservationService reservationService;

    ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @PostMapping("/api/sales/{saleId}/reservations")
    ResponseEntity<ReservationResponse> hold(
            @PathVariable UUID saleId, @Valid @RequestBody HoldTicketsRequest request) {
        Reservation reservation =
                reservationService.hold(
                        new HoldTicketsCommand(saleId, request.customerId(), request.quantity()));
        return ResponseEntity.created(URI.create("/api/reservations/" + reservation.getId()))
                .body(ReservationResponse.from(reservation));
    }

    @GetMapping("/api/reservations/{reservationId}")
    ReservationResponse get(@PathVariable UUID reservationId) {
        return ReservationResponse.from(reservationService.get(new ReservationId(reservationId)));
    }
}
