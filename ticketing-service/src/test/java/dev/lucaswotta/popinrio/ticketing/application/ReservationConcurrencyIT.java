package dev.lucaswotta.popinrio.ticketing.application;

import static org.assertj.core.api.Assertions.assertThat;

import dev.lucaswotta.popinrio.ticketing.domain.sale.Sale;
import dev.lucaswotta.popinrio.ticketing.domain.sale.SoldOutException;
import dev.lucaswotta.popinrio.ticketing.support.IntegrationTest;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntFunction;

/**
 * A prova central do projeto: muitos compradores disparando ao mesmo tempo contra o mesmo estoque,
 * num PostgreSQL real, e o sistema nunca vende mais do que existe.
 */
class ReservationConcurrencyIT extends IntegrationTest {

    @Autowired private SaleService saleService;
    @Autowired private ReservationService reservationService;

    @Test
    void neverSellsMoreThanTheStockWhenBuyersOutnumberTickets() throws InterruptedException {
        int tickets = 100;
        int buyers = 500;
        Sale sale = saleService.open(new OpenSaleCommand("Pop in Rio - Noite 1", tickets));
        UUID saleId = sale.getId().value();

        RaceResult result = race(buyers, i -> new HoldTicketsCommand(saleId, "buyer-" + i, 1));

        assertThat(result.unexpectedErrors()).isEmpty();
        assertThat(result.held()).isEqualTo(tickets);
        assertThat(result.soldOut()).isEqualTo(buyers - tickets);
        assertThat(availableTickets(saleId)).isZero();
        assertThat(reservedTickets(saleId)).isEqualTo(tickets);
    }

    @Test
    void stockAlwaysMatchesReservationsWithMixedQuantities() throws InterruptedException {
        int tickets = 250;
        int buyers = 300;
        Sale sale = saleService.open(new OpenSaleCommand("Pop in Rio - Noite 2", tickets));
        UUID saleId = sale.getId().value();

        RaceResult result =
                race(buyers, i -> new HoldTicketsCommand(saleId, "buyer-" + i, i % 4 + 1));

        assertThat(result.unexpectedErrors()).isEmpty();
        assertThat(availableTickets(saleId)).isNotNegative();
        // Nenhum ingresso some nem aparece do nada: estoque + reservado = total.
        assertThat(availableTickets(saleId) + reservedTickets(saleId)).isEqualTo(tickets);
        assertThat(result.heldTickets()).isEqualTo(reservedTickets(saleId));
    }

    /**
     * Prepara todas as threads e solta todas juntas com um CountDownLatch, para maximizar a disputa
     * pela mesma linha. Virtual threads deixam criar centenas de compradores sem custo.
     */
    private RaceResult race(int buyers, IntFunction<HoldTicketsCommand> commandFor)
            throws InterruptedException {
        CountDownLatch ready = new CountDownLatch(buyers);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger held = new AtomicInteger();
        AtomicInteger heldTickets = new AtomicInteger();
        AtomicInteger soldOut = new AtomicInteger();
        ConcurrentLinkedQueue<Throwable> unexpected = new ConcurrentLinkedQueue<>();

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < buyers; i++) {
                HoldTicketsCommand command = commandFor.apply(i);
                executor.submit(
                        () -> {
                            ready.countDown();
                            try {
                                start.await();
                                int quantity =
                                        reservationService.hold(command).getQuantity().value();
                                held.incrementAndGet();
                                heldTickets.addAndGet(quantity);
                            } catch (SoldOutException e) {
                                soldOut.incrementAndGet();
                            } catch (Throwable e) {
                                unexpected.add(e);
                            }
                        });
            }
            assertThat(ready.await(30, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            executor.shutdown();
            assertThat(executor.awaitTermination(60, TimeUnit.SECONDS)).isTrue();
        }

        return new RaceResult(
                held.get(), heldTickets.get(), soldOut.get(), List.copyOf(unexpected));
    }

    private int availableTickets(UUID saleId) {
        return jdbc.queryForObject(
                "select available_tickets from sale where id = ?", Integer.class, saleId);
    }

    private int reservedTickets(UUID saleId) {
        return jdbc.queryForObject(
                "select coalesce(sum(quantity), 0) from reservation where sale_id = ?",
                Integer.class,
                saleId);
    }

    private record RaceResult(
            int held, int heldTickets, int soldOut, List<Throwable> unexpectedErrors) {}
}
