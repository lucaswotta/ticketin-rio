package dev.lucaswotta.popinrio.ticketing.application;

import java.util.UUID;

public record HoldTicketsCommand(UUID saleId, String customerId, int quantity) {}
