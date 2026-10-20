package dev.lucaswotta.popinrio.ticketing.domain.reservation;

import dev.lucaswotta.popinrio.ticketing.domain.reservation.ReservationEvents.ReservationCancelled;
import dev.lucaswotta.popinrio.ticketing.domain.reservation.ReservationEvents.ReservationConfirmed;
import dev.lucaswotta.popinrio.ticketing.domain.reservation.ReservationEvents.ReservationExpired;
import dev.lucaswotta.popinrio.ticketing.domain.reservation.ReservationEvents.ReservationHeld;
import dev.lucaswotta.popinrio.ticketing.domain.sale.InvalidQuantityException;
import dev.lucaswotta.popinrio.ticketing.domain.sale.SaleId;
import dev.lucaswotta.popinrio.ticketing.domain.sale.TicketQuantity;
import dev.lucaswotta.popinrio.ticketing.domain.shared.AggregateRoot;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * Agregado Reserva: ingressos segurados para um comprador por tempo limitado.
 *
 * <p>É um agregado separado de {@code Sale} de propósito, e referencia a venda só pelo ID. Se as
 * reservas morassem dentro da venda, toda reserva travaria o agregado inteiro, e mil compradores
 * disputariam um único objeto. Separados, só o contador de estoque é disputado (ver ADR 0002).
 *
 * <p>Ciclo de vida: {@code HELD -> CONFIRMED | EXPIRED | CANCELLED}, todos terminais.
 */
public final class Reservation extends AggregateRoot {

    public static final int MAX_TICKETS_PER_RESERVATION = 4;

    private final ReservationId id;
    private final SaleId saleId;
    private final CustomerId customerId;
    private final TicketQuantity quantity;
    private ReservationStatus status;
    private final Instant createdAt;
    private final Instant expiresAt;

    private Reservation(
            ReservationId id,
            SaleId saleId,
            CustomerId customerId,
            TicketQuantity quantity,
            ReservationStatus status,
            Instant createdAt,
            Instant expiresAt) {
        this.id = id;
        this.saleId = saleId;
        this.customerId = customerId;
        this.quantity = quantity;
        this.status = status;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    /**
     * Cria a reserva. Pressupõe que os ingressos já foram retirados do estoque: a ordem (retirar,
     * depois criar) é garantida pelo caso de uso, dentro da mesma transação.
     */
    public static Reservation hold(
            SaleId saleId,
            CustomerId customerId,
            TicketQuantity quantity,
            Instant now,
            Duration holdDuration) {
        Objects.requireNonNull(saleId, "saleId");
        Objects.requireNonNull(customerId, "customerId");
        Objects.requireNonNull(now, "now");
        requireAllowedQuantity(quantity);
        if (holdDuration == null || holdDuration.isNegative() || holdDuration.isZero()) {
            throw new IllegalArgumentException("holdDuration deve ser positiva");
        }

        Reservation reservation =
                new Reservation(
                        ReservationId.newId(),
                        saleId,
                        customerId,
                        quantity,
                        ReservationStatus.HELD,
                        now,
                        now.plus(holdDuration));
        reservation.registerEvent(
                new ReservationHeld(
                        reservation.id,
                        saleId,
                        customerId,
                        quantity.value(),
                        reservation.expiresAt,
                        now));
        return reservation;
    }

    public static Reservation restore(
            ReservationId id,
            SaleId saleId,
            CustomerId customerId,
            TicketQuantity quantity,
            ReservationStatus status,
            Instant createdAt,
            Instant expiresAt) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(saleId, "saleId");
        Objects.requireNonNull(customerId, "customerId");
        Objects.requireNonNull(quantity, "quantity");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(createdAt, "createdAt");
        Objects.requireNonNull(expiresAt, "expiresAt");
        return new Reservation(id, saleId, customerId, quantity, status, createdAt, expiresAt);
    }

    /**
     * Regra de negócio exposta para validar antes de tocar no estoque: barrar uma quantidade
     * inválida depois do UPDATE custaria um rollback desnecessário.
     */
    public static void requireAllowedQuantity(TicketQuantity quantity) {
        Objects.requireNonNull(quantity, "quantity");
        if (quantity.isGreaterThan(MAX_TICKETS_PER_RESERVATION)) {
            throw new InvalidQuantityException(
                    "Máximo de " + MAX_TICKETS_PER_RESERVATION + " ingressos por reserva");
        }
    }

    /** O prazo vence no instante exato de {@code expiresAt}, não depois. */
    public boolean isExpiredAt(Instant now) {
        return !now.isBefore(expiresAt);
    }

    public void confirm(Instant now) {
        requireHeld(ReservationStatus.CONFIRMED);
        if (isExpiredAt(now)) {
            throw new InvalidReservationTransitionException(
                    id, status, ReservationStatus.CONFIRMED, "o prazo da reserva venceu");
        }
        status = ReservationStatus.CONFIRMED;
        registerEvent(new ReservationConfirmed(id, saleId, quantity.value(), now));
    }

    public void expire(Instant now) {
        requireHeld(ReservationStatus.EXPIRED);
        if (!isExpiredAt(now)) {
            throw new InvalidReservationTransitionException(
                    id, status, ReservationStatus.EXPIRED, "o prazo ainda não venceu");
        }
        status = ReservationStatus.EXPIRED;
        registerEvent(new ReservationExpired(id, saleId, quantity.value(), now));
    }

    public void cancel(Instant now) {
        requireHeld(ReservationStatus.CANCELLED);
        status = ReservationStatus.CANCELLED;
        registerEvent(new ReservationCancelled(id, saleId, quantity.value(), now));
    }

    private void requireHeld(ReservationStatus target) {
        if (status != ReservationStatus.HELD) {
            throw new InvalidReservationTransitionException(
                    id, status, target, "a reserva já está finalizada");
        }
    }

    public ReservationId getId() {
        return id;
    }

    public SaleId getSaleId() {
        return saleId;
    }

    public CustomerId getCustomerId() {
        return customerId;
    }

    public TicketQuantity getQuantity() {
        return quantity;
    }

    public ReservationStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    @Override
    public boolean equals(Object o) {
        return this == o || (o instanceof Reservation other && id.equals(other.id));
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
