package dev.lucaswotta.popinrio.ticketing.domain.sale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import java.time.Instant;

class SaleTest {

    private static final Instant NOW = Instant.parse("2026-10-20T12:00:00Z");

    @Test
    void openStartsWithAllTicketsAvailable() {
        Sale sale = Sale.open("  Pop in Rio - Noite 1  ", 100, NOW);

        assertThat(sale.getName()).isEqualTo("Pop in Rio - Noite 1");
        assertThat(sale.getTotalTickets()).isEqualTo(100);
        assertThat(sale.getAvailableTickets()).isEqualTo(100);
        assertThat(sale.soldTickets()).isZero();
        assertThat(sale.isSoldOut()).isFalse();
    }

    @Test
    void rejectsBlankName() {
        assertThatThrownBy(() -> Sale.open("   ", 100, NOW))
                .isInstanceOf(InvalidSaleException.class);
    }

    @Test
    void rejectsTotalOutsideAllowedRange() {
        assertThatThrownBy(() -> Sale.open("Show", 0, NOW))
                .isInstanceOf(InvalidSaleException.class);
        assertThatThrownBy(() -> Sale.open("Show", Sale.MAX_TOTAL_TICKETS + 1, NOW))
                .isInstanceOf(InvalidSaleException.class);
    }

    @Test
    void withdrawDecreasesAvailableTickets() {
        Sale sale = Sale.open("Show", 10, NOW);

        sale.withdraw(TicketQuantity.of(3));

        assertThat(sale.getAvailableTickets()).isEqualTo(7);
        assertThat(sale.soldTickets()).isEqualTo(3);
    }

    @Test
    void withdrawingExactlyTheRemainingStockSellsOut() {
        Sale sale = Sale.open("Show", 4, NOW);

        sale.withdraw(TicketQuantity.of(4));

        assertThat(sale.isSoldOut()).isTrue();
    }

    @Test
    void withdrawingMoreThanAvailableFailsAndKeepsStockUntouched() {
        Sale sale = Sale.open("Show", 2, NOW);

        assertThatThrownBy(() -> sale.withdraw(TicketQuantity.of(3)))
                .isInstanceOf(SoldOutException.class);
        assertThat(sale.getAvailableTickets()).isEqualTo(2);
    }

    @Test
    void restoreRejectsCorruptedStock() {
        assertThatThrownBy(() -> Sale.restore(SaleId.newId(), "Show", 10, 11, NOW))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> Sale.restore(SaleId.newId(), "Show", 10, -1, NOW))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void ticketQuantityMustBePositive() {
        assertThatThrownBy(() -> TicketQuantity.of(0)).isInstanceOf(InvalidQuantityException.class);
        assertThatThrownBy(() -> TicketQuantity.of(-1))
                .isInstanceOf(InvalidQuantityException.class);
    }
}
