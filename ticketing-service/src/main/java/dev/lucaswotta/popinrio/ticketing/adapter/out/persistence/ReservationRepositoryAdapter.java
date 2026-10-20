package dev.lucaswotta.popinrio.ticketing.adapter.out.persistence;

import dev.lucaswotta.popinrio.ticketing.domain.reservation.Reservation;
import dev.lucaswotta.popinrio.ticketing.domain.reservation.ReservationId;
import dev.lucaswotta.popinrio.ticketing.domain.reservation.ReservationRepository;

import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
class ReservationRepositoryAdapter implements ReservationRepository {

    private final SpringDataReservationRepository jpa;

    ReservationRepositoryAdapter(SpringDataReservationRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public void save(Reservation reservation) {
        jpa.findById(reservation.getId().value())
                .ifPresentOrElse(
                        entity -> entity.updateFrom(reservation),
                        () -> jpa.save(ReservationJpaEntity.newFrom(reservation)));
    }

    @Override
    public Optional<Reservation> findById(ReservationId id) {
        return jpa.findById(id.value()).map(ReservationJpaEntity::toDomain);
    }
}
