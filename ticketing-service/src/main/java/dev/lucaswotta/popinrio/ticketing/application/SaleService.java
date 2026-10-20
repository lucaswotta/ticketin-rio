package dev.lucaswotta.popinrio.ticketing.application;

import dev.lucaswotta.popinrio.ticketing.domain.sale.Sale;
import dev.lucaswotta.popinrio.ticketing.domain.sale.SaleId;
import dev.lucaswotta.popinrio.ticketing.domain.sale.SaleNotFoundException;
import dev.lucaswotta.popinrio.ticketing.domain.sale.SaleRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

@Service
public class SaleService {

    private final SaleRepository saleRepository;
    private final Clock clock;

    public SaleService(SaleRepository saleRepository, Clock clock) {
        this.saleRepository = saleRepository;
        this.clock = clock;
    }

    @Transactional
    public Sale open(OpenSaleCommand command) {
        Sale sale = Sale.open(command.name(), command.totalTickets(), clock.instant());
        saleRepository.save(sale);
        return sale;
    }

    @Transactional(readOnly = true)
    public Sale get(SaleId id) {
        return saleRepository.findById(id).orElseThrow(() -> new SaleNotFoundException(id));
    }
}
