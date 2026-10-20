package dev.lucaswotta.popinrio.ticketing.adapter.in.web;

import dev.lucaswotta.popinrio.ticketing.domain.reservation.CustomerId;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

record HoldTicketsRequest(
        @NotBlank @Size(max = CustomerId.MAX_LENGTH) String customerId,
        @NotNull Integer quantity) {}
