package dev.lucaswotta.popinrio.ticketing.domain.sale;

import java.util.Optional;

/** Repositório do agregado Sale. A interface é do domínio; a implementação JPA é um adaptador. */
public interface SaleRepository {

    void save(Sale sale);

    Optional<Sale> findById(SaleId id);

    boolean existsById(SaleId id);
}
