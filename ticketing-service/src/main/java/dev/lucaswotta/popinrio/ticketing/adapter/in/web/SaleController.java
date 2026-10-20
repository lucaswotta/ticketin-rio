package dev.lucaswotta.popinrio.ticketing.adapter.in.web;

import dev.lucaswotta.popinrio.ticketing.application.OpenSaleCommand;
import dev.lucaswotta.popinrio.ticketing.application.SaleService;
import dev.lucaswotta.popinrio.ticketing.domain.sale.Sale;
import dev.lucaswotta.popinrio.ticketing.domain.sale.SaleId;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/sales")
class SaleController {

    private final SaleService saleService;

    SaleController(SaleService saleService) {
        this.saleService = saleService;
    }

    @PostMapping
    ResponseEntity<SaleResponse> open(@Valid @RequestBody OpenSaleRequest request) {
        Sale sale = saleService.open(new OpenSaleCommand(request.name(), request.totalTickets()));
        return ResponseEntity.created(URI.create("/api/sales/" + sale.getId()))
                .body(SaleResponse.from(sale));
    }

    @GetMapping("/{saleId}")
    SaleResponse get(@PathVariable UUID saleId) {
        return SaleResponse.from(saleService.get(new SaleId(saleId)));
    }
}
