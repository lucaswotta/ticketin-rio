package dev.lucaswotta.popinrio.ticketing.adapter.in.web;

import dev.lucaswotta.popinrio.ticketing.domain.sale.Sale;

import java.time.Instant;
import java.util.UUID;

record SaleResponse(
        UUID id,
        String name,
        int totalTickets,
        int availableTickets,
        int soldTickets,
        Instant createdAt) {

    static SaleResponse from(Sale sale) {
        return new SaleResponse(
                sale.getId().value(),
                sale.getName(),
                sale.getTotalTickets(),
                sale.getAvailableTickets(),
                sale.soldTickets(),
                sale.getCreatedAt());
    }
}
