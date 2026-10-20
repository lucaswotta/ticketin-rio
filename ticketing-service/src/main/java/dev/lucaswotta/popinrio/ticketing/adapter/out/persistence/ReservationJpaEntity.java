package dev.lucaswotta.popinrio.ticketing.adapter.out.persistence;

import dev.lucaswotta.popinrio.ticketing.domain.reservation.CustomerId;
import dev.lucaswotta.popinrio.ticketing.domain.reservation.Reservation;
import dev.lucaswotta.popinrio.ticketing.domain.reservation.ReservationId;
import dev.lucaswotta.popinrio.ticketing.domain.reservation.ReservationStatus;
import dev.lucaswotta.popinrio.ticketing.domain.sale.SaleId;
import dev.lucaswotta.popinrio.ticketing.domain.sale.TicketQuantity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import org.springframework.data.domain.Persistable;

import java.time.Instant;
import java.util.UUID;

/**
 * A reserva guarda só o ID da venda, sem {@code @ManyToOne}. Agregados se referenciam por
 * identidade: carregar a venda junto com cada reserva acoplaria os dois e convidaria o problema
 * N+1.
 */
@Entity
@Table(name = "reservation")
class ReservationJpaEntity implements Persistable<UUID> {

    @Id private UUID id;

    @Column(name = "sale_id", nullable = false, updatable = false)
    private UUID saleId;

    @Column(
            name = "customer_id",
            nullable = false,
            updatable = false,
            length = CustomerId.MAX_LENGTH)
    private String customerId;

    @Column(nullable = false, updatable = false)
    private int quantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReservationStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false, updatable = false)
    private Instant expiresAt;

    @Transient private boolean isNew;

    protected ReservationJpaEntity() {}

    static ReservationJpaEntity newFrom(Reservation reservation) {
        ReservationJpaEntity entity = new ReservationJpaEntity();
        entity.id = reservation.getId().value();
        entity.saleId = reservation.getSaleId().value();
        entity.customerId = reservation.getCustomerId().value();
        entity.quantity = reservation.getQuantity().value();
        entity.status = reservation.getStatus();
        entity.createdAt = reservation.getCreatedAt();
        entity.expiresAt = reservation.getExpiresAt();
        entity.isNew = true;
        return entity;
    }

    void updateFrom(Reservation reservation) {
        this.status = reservation.getStatus();
    }

    Reservation toDomain() {
        return Reservation.restore(
                new ReservationId(id),
                new SaleId(saleId),
                new CustomerId(customerId),
                TicketQuantity.of(quantity),
                status,
                createdAt,
                expiresAt);
    }

    @PostLoad
    @PostPersist
    void markNotNew() {
        this.isNew = false;
    }

    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }
}
