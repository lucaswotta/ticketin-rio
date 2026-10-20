package dev.lucaswotta.popinrio.ticketing.domain.reservation;

import java.util.Optional;

public interface ReservationRepository {

    void save(Reservation reservation);

    Optional<Reservation> findById(ReservationId id);
}
