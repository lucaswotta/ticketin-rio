package dev.lucaswotta.popinrio.ticketing.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.lucaswotta.popinrio.ticketing.domain.reservation.Reservation;
import dev.lucaswotta.popinrio.ticketing.domain.reservation.ReservationEvents.ReservationHeld;
import dev.lucaswotta.popinrio.ticketing.domain.reservation.ReservationId;
import dev.lucaswotta.popinrio.ticketing.domain.reservation.ReservationRepository;
import dev.lucaswotta.popinrio.ticketing.domain.reservation.ReservationStatus;
import dev.lucaswotta.popinrio.ticketing.domain.sale.InvalidQuantityException;
import dev.lucaswotta.popinrio.ticketing.domain.sale.Sale;
import dev.lucaswotta.popinrio.ticketing.domain.sale.SaleId;
import dev.lucaswotta.popinrio.ticketing.domain.sale.SaleNotFoundException;
import dev.lucaswotta.popinrio.ticketing.domain.sale.SaleRepository;
import dev.lucaswotta.popinrio.ticketing.domain.sale.SoldOutException;
import dev.lucaswotta.popinrio.ticketing.domain.sale.TicketInventory;
import dev.lucaswotta.popinrio.ticketing.domain.sale.TicketQuantity;
import dev.lucaswotta.popinrio.ticketing.domain.shared.DomainEvent;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Testa o caso de uso sem Spring e sem banco, usando implementações em memória das portas. A
 * concorrência real é provada em {@code ReservationConcurrencyIT}, contra PostgreSQL.
 */
class ReservationServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-20T12:00:00Z");
    private static final Duration HOLD = Duration.ofMinutes(10);

    private InMemorySales sales;
    private InMemoryReservations reservations;
    private List<DomainEvent> published;
    private ReservationService service;

    @BeforeEach
    void setUp() {
        sales = new InMemorySales();
        reservations = new InMemoryReservations();
        published = new ArrayList<>();
        service =
                new ReservationService(
                        sales,
                        sales,
                        reservations,
                        published::addAll,
                        new ReservationProperties(HOLD),
                        Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private Sale openSale(int tickets) {
        Sale sale = Sale.open("Pop in Rio", tickets, NOW);
        sales.save(sale);
        return sale;
    }

    @Test
    void holdWithdrawsStockSavesReservationAndPublishesEvent() {
        Sale sale = openSale(10);

        Reservation reservation =
                service.hold(new HoldTicketsCommand(sale.getId().value(), "lucas", 3));

        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.HELD);
        assertThat(reservation.getExpiresAt()).isEqualTo(NOW.plus(HOLD));
        assertThat(sales.availableOf(sale.getId())).isEqualTo(7);
        assertThat(reservations.findById(reservation.getId())).isPresent();
        assertThat(published).singleElement().isInstanceOf(ReservationHeld.class);
    }

    @Test
    void soldOutWhenStockIsInsufficient() {
        Sale sale = openSale(2);

        assertThatThrownBy(
                        () ->
                                service.hold(
                                        new HoldTicketsCommand(sale.getId().value(), "lucas", 3)))
                .isInstanceOf(SoldOutException.class);
        assertThat(sales.availableOf(sale.getId())).isEqualTo(2);
        assertThat(reservations.count()).isZero();
        assertThat(published).isEmpty();
    }

    @Test
    void notFoundWhenSaleDoesNotExist() {
        assertThatThrownBy(
                        () -> service.hold(new HoldTicketsCommand(UUID.randomUUID(), "lucas", 1)))
                .isInstanceOf(SaleNotFoundException.class);
    }

    @Test
    void quantityAboveLimitIsRejectedBeforeTouchingStock() {
        Sale sale = openSale(10);
        int tooMany = Reservation.MAX_TICKETS_PER_RESERVATION + 1;

        assertThatThrownBy(
                        () ->
                                service.hold(
                                        new HoldTicketsCommand(
                                                sale.getId().value(), "lucas", tooMany)))
                .isInstanceOf(InvalidQuantityException.class);
        assertThat(sales.withdrawCalls).isZero();
    }

    /** Venda em memória fazendo papel de repositório e de estoque. */
    private static final class InMemorySales implements SaleRepository, TicketInventory {

        private final Map<SaleId, Sale> store = new HashMap<>();
        private int withdrawCalls;

        @Override
        public void save(Sale sale) {
            store.put(sale.getId(), sale);
        }

        @Override
        public Optional<Sale> findById(SaleId id) {
            return Optional.ofNullable(store.get(id));
        }

        @Override
        public boolean existsById(SaleId id) {
            return store.containsKey(id);
        }

        @Override
        public boolean tryWithdraw(SaleId saleId, TicketQuantity quantity) {
            withdrawCalls++;
            Sale sale = store.get(saleId);
            if (sale == null || quantity.isGreaterThan(sale.getAvailableTickets())) {
                return false;
            }
            sale.withdraw(quantity);
            return true;
        }

        int availableOf(SaleId id) {
            return store.get(id).getAvailableTickets();
        }
    }

    private static final class InMemoryReservations implements ReservationRepository {

        private final Map<ReservationId, Reservation> store = new HashMap<>();

        @Override
        public void save(Reservation reservation) {
            store.put(reservation.getId(), reservation);
        }

        @Override
        public Optional<Reservation> findById(ReservationId id) {
            return Optional.ofNullable(store.get(id));
        }

        int count() {
            return store.size();
        }
    }
}
