package dev.lucaswotta.popinrio.ticketing.domain.reservation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.lucaswotta.popinrio.ticketing.domain.reservation.ReservationEvents.ReservationCancelled;
import dev.lucaswotta.popinrio.ticketing.domain.reservation.ReservationEvents.ReservationConfirmed;
import dev.lucaswotta.popinrio.ticketing.domain.reservation.ReservationEvents.ReservationExpired;
import dev.lucaswotta.popinrio.ticketing.domain.reservation.ReservationEvents.ReservationHeld;
import dev.lucaswotta.popinrio.ticketing.domain.sale.InvalidQuantityException;
import dev.lucaswotta.popinrio.ticketing.domain.sale.SaleId;
import dev.lucaswotta.popinrio.ticketing.domain.sale.TicketQuantity;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

class ReservationTest {

    private static final Instant NOW = Instant.parse("2026-10-20T12:00:00Z");
    private static final Duration HOLD = Duration.ofMinutes(10);
    private static final SaleId SALE = SaleId.newId();
    private static final CustomerId CUSTOMER = new CustomerId("lucas");

    private Reservation held() {
        Reservation reservation = Reservation.hold(SALE, CUSTOMER, TicketQuantity.of(2), NOW, HOLD);
        reservation.pullDomainEvents();
        return reservation;
    }

    @Test
    void holdStartsHeldWithDeadlineAndRegistersEvent() {
        Reservation reservation = Reservation.hold(SALE, CUSTOMER, TicketQuantity.of(2), NOW, HOLD);

        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.HELD);
        assertThat(reservation.getExpiresAt()).isEqualTo(NOW.plus(HOLD));
        assertThat(reservation.pullDomainEvents())
                .singleElement()
                .isInstanceOfSatisfying(
                        ReservationHeld.class,
                        event -> {
                            assertThat(event.reservationId()).isEqualTo(reservation.getId());
                            assertThat(event.quantity()).isEqualTo(2);
                        });
    }

    @Test
    void pullingEventsEmptiesTheList() {
        Reservation reservation = Reservation.hold(SALE, CUSTOMER, TicketQuantity.of(1), NOW, HOLD);

        reservation.pullDomainEvents();

        assertThat(reservation.pullDomainEvents()).isEmpty();
    }

    @Test
    void rejectsMoreThanTheLimitPerReservation() {
        TicketQuantity tooMany = TicketQuantity.of(Reservation.MAX_TICKETS_PER_RESERVATION + 1);

        assertThatThrownBy(() -> Reservation.hold(SALE, CUSTOMER, tooMany, NOW, HOLD))
                .isInstanceOf(InvalidQuantityException.class);
    }

    @Test
    void confirmWithinDeadline() {
        Reservation reservation = held();

        reservation.confirm(NOW.plus(HOLD).minusSeconds(1));

        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
        assertThat(reservation.pullDomainEvents())
                .singleElement()
                .isInstanceOf(ReservationConfirmed.class);
    }

    @Test
    void cannotConfirmAtTheExactDeadline() {
        Reservation reservation = held();

        assertThatThrownBy(() -> reservation.confirm(NOW.plus(HOLD)))
                .isInstanceOf(InvalidReservationTransitionException.class);
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.HELD);
    }

    @Test
    void expireOnlyAfterDeadline() {
        Reservation reservation = held();

        assertThatThrownBy(() -> reservation.expire(NOW.plusSeconds(1)))
                .isInstanceOf(InvalidReservationTransitionException.class);

        reservation.expire(NOW.plus(HOLD));

        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.EXPIRED);
        assertThat(reservation.pullDomainEvents())
                .singleElement()
                .isInstanceOf(ReservationExpired.class);
    }

    @Test
    void cancelWhileHeld() {
        Reservation reservation = held();

        reservation.cancel(NOW);

        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
        assertThat(reservation.pullDomainEvents())
                .singleElement()
                .isInstanceOf(ReservationCancelled.class);
    }

    @Test
    void finalStatesAreTerminal() {
        Reservation reservation = held();
        reservation.confirm(NOW);

        assertThatThrownBy(() -> reservation.cancel(NOW))
                .isInstanceOf(InvalidReservationTransitionException.class);
        assertThatThrownBy(() -> reservation.expire(NOW.plus(HOLD)))
                .isInstanceOf(InvalidReservationTransitionException.class);
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
    }

    @Test
    void customerIdIsTrimmedAndBounded() {
        assertThat(new CustomerId("  lucas  ").value()).isEqualTo("lucas");
        assertThatThrownBy(() -> new CustomerId(" ")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new CustomerId("x".repeat(CustomerId.MAX_LENGTH + 1)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
