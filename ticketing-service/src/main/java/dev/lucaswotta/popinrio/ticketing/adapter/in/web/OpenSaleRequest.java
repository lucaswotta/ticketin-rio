package dev.lucaswotta.popinrio.ticketing.adapter.in.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Validação na borda só checa formato (campo presente). Regra de negócio, como o limite de
 * ingressos, é do domínio: 400 é "requisição malformada", 422 é "regra de negócio violada".
 */
record OpenSaleRequest(@NotBlank String name, @NotNull Integer totalTickets) {}
