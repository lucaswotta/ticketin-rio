package dev.lucaswotta.popinrio.ticketing.adapter.out.persistence;

import dev.lucaswotta.popinrio.ticketing.domain.sale.Sale;
import dev.lucaswotta.popinrio.ticketing.domain.sale.SaleId;
import dev.lucaswotta.popinrio.ticketing.domain.sale.SaleRepository;

import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
class SaleRepositoryAdapter implements SaleRepository {

    private final SpringDataSaleRepository jpa;

    SaleRepositoryAdapter(SpringDataSaleRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public void save(Sale sale) {
        jpa.findById(sale.getId().value())
                .ifPresentOrElse(
                        entity -> entity.updateFrom(sale),
                        () -> jpa.save(SaleJpaEntity.newFrom(sale)));
    }

    @Override
    public Optional<Sale> findById(SaleId id) {
        return jpa.findById(id.value()).map(SaleJpaEntity::toDomain);
    }

    @Override
    public boolean existsById(SaleId id) {
        return jpa.existsById(id.value());
    }
}
