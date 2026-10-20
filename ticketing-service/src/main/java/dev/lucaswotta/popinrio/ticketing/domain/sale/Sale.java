package dev.lucaswotta.popinrio.ticketing.domain.sale;

import dev.lucaswotta.popinrio.ticketing.domain.shared.AggregateRoot;

import java.time.Instant;
import java.util.Objects;

/**
 * Agregado Venda: um show com um estoque fixo de ingressos.
 *
 * <p>Invariante: {@code 0 <= availableTickets <= totalTickets}. O banco reforça a mesma regra com
 * uma CHECK constraint, como última linha de defesa caso alguma estratégia de concorrência falhe.
 */
public final class Sale extends AggregateRoot {

    public static final int MAX_TOTAL_TICKETS = 100_000;
    public static final int MAX_NAME_LENGTH = 120;

    private final SaleId id;
    private final String name;
    private final int totalTickets;
    private int availableTickets;
    private final Instant createdAt;

    private Sale(
            SaleId id, String name, int totalTickets, int availableTickets, Instant createdAt) {
        this.id = id;
        this.name = name;
        this.totalTickets = totalTickets;
        this.availableTickets = availableTickets;
        this.createdAt = createdAt;
    }

    public static Sale open(String name, int totalTickets, Instant now) {
        Objects.requireNonNull(now, "now");
        if (name == null || name.isBlank()) {
            throw new InvalidSaleException("O nome da venda é obrigatório");
        }
        if (name.strip().length() > MAX_NAME_LENGTH) {
            throw new InvalidSaleException(
                    "O nome da venda deve ter no máximo " + MAX_NAME_LENGTH + " caracteres");
        }
        if (totalTickets < 1 || totalTickets > MAX_TOTAL_TICKETS) {
            throw new InvalidSaleException(
                    "O total de ingressos deve estar entre 1 e " + MAX_TOTAL_TICKETS);
        }
        return new Sale(SaleId.newId(), name.strip(), totalTickets, totalTickets, now);
    }

    /** Reconstrói a partir do banco. Estado inconsistente aqui é bug, não erro de usuário. */
    public static Sale restore(
            SaleId id, String name, int totalTickets, int availableTickets, Instant createdAt) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(createdAt, "createdAt");
        if (availableTickets < 0 || availableTickets > totalTickets) {
            throw new IllegalStateException(
                    "Estado corrompido na venda %s: disponíveis=%d, total=%d"
                            .formatted(id, availableTickets, totalTickets));
        }
        return new Sale(id, name, totalTickets, availableTickets, createdAt);
    }

    /**
     * Retira ingressos do estoque em memória. Só é seguro sob concorrência se quem chama tiver
     * exclusividade sobre a linha no banco (lock pessimista ou versão otimista). A estratégia de
     * UPDATE atômico não passa por aqui: ela aplica a mesma regra direto no SQL.
     */
    public void withdraw(TicketQuantity quantity) {
        Objects.requireNonNull(quantity, "quantity");
        if (quantity.isGreaterThan(availableTickets)) {
            throw new SoldOutException(id, quantity);
        }
        availableTickets -= quantity.value();
    }

    public boolean isSoldOut() {
        return availableTickets == 0;
    }

    public int soldTickets() {
        return totalTickets - availableTickets;
    }

    public SaleId getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getTotalTickets() {
        return totalTickets;
    }

    public int getAvailableTickets() {
        return availableTickets;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    @Override
    public boolean equals(Object o) {
        return this == o || (o instanceof Sale other && id.equals(other.id));
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
