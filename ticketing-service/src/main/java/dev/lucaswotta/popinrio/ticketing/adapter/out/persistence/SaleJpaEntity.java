package dev.lucaswotta.popinrio.ticketing.adapter.out.persistence;

import dev.lucaswotta.popinrio.ticketing.domain.sale.Sale;
import dev.lucaswotta.popinrio.ticketing.domain.sale.SaleId;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import org.springframework.data.domain.Persistable;

import java.time.Instant;
import java.util.UUID;

/**
 * Representação da venda no banco. É separada do agregado {@link Sale} para que o domínio não
 * carregue anotações de JPA nem precise de construtor vazio e setters.
 *
 * <p>Implementa {@link Persistable} porque o ID é gerado pela aplicação: sem isso, o Spring Data
 * enxerga um ID preenchido, assume que a entidade já existe e faz um SELECT antes de cada INSERT.
 */
@Entity
@Table(name = "sale")
class SaleJpaEntity implements Persistable<UUID> {

    @Id private UUID id;

    @Column(nullable = false, length = Sale.MAX_NAME_LENGTH)
    private String name;

    @Column(name = "total_tickets", nullable = false)
    private int totalTickets;

    @Column(name = "available_tickets", nullable = false)
    private int availableTickets;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Transient private boolean isNew;

    protected SaleJpaEntity() {}

    static SaleJpaEntity newFrom(Sale sale) {
        SaleJpaEntity entity = new SaleJpaEntity();
        entity.id = sale.getId().value();
        entity.name = sale.getName();
        entity.totalTickets = sale.getTotalTickets();
        entity.availableTickets = sale.getAvailableTickets();
        entity.createdAt = sale.getCreatedAt();
        entity.isNew = true;
        return entity;
    }

    void updateFrom(Sale sale) {
        this.availableTickets = sale.getAvailableTickets();
    }

    Sale toDomain() {
        return Sale.restore(new SaleId(id), name, totalTickets, availableTickets, createdAt);
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
