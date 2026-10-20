package dev.lucaswotta.popinrio.ticketing.application;

import dev.lucaswotta.popinrio.ticketing.domain.reservation.CustomerId;
import dev.lucaswotta.popinrio.ticketing.domain.reservation.Reservation;
import dev.lucaswotta.popinrio.ticketing.domain.reservation.ReservationId;
import dev.lucaswotta.popinrio.ticketing.domain.reservation.ReservationNotFoundException;
import dev.lucaswotta.popinrio.ticketing.domain.reservation.ReservationRepository;
import dev.lucaswotta.popinrio.ticketing.domain.sale.SaleId;
import dev.lucaswotta.popinrio.ticketing.domain.sale.SaleNotFoundException;
import dev.lucaswotta.popinrio.ticketing.domain.sale.SaleRepository;
import dev.lucaswotta.popinrio.ticketing.domain.sale.SoldOutException;
import dev.lucaswotta.popinrio.ticketing.domain.sale.TicketInventory;
import dev.lucaswotta.popinrio.ticketing.domain.sale.TicketQuantity;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

@Service
public class ReservationService {

    private final TicketInventory ticketInventory;
    private final SaleRepository saleRepository;
    private final ReservationRepository reservationRepository;
    private final DomainEventPublisher eventPublisher;
    private final ReservationProperties properties;
    private final Clock clock;

    public ReservationService(
            TicketInventory ticketInventory,
            SaleRepository saleRepository,
            ReservationRepository reservationRepository,
            DomainEventPublisher eventPublisher,
            ReservationProperties properties,
            Clock clock) {
        this.ticketInventory = ticketInventory;
        this.saleRepository = saleRepository;
        this.reservationRepository = reservationRepository;
        this.eventPublisher = eventPublisher;
        this.properties = properties;
        this.clock = clock;
    }

    /**
     * Segura ingressos para um comprador.
     *
     * <p>A retirada do estoque e a gravação da reserva acontecem na mesma transação. Se gravar a
     * reserva falhar, o rollback devolve os ingressos ao estoque: nunca existe estoque retirado sem
     * reserva correspondente, nem o contrário.
     */
    @Transactional
    public Reservation hold(HoldTicketsCommand command) {
        SaleId saleId = new SaleId(command.saleId());
        CustomerId customerId = new CustomerId(command.customerId());
        TicketQuantity quantity = TicketQuantity.of(command.quantity());
        Reservation.requireAllowedQuantity(quantity);

        if (!ticketInventory.tryWithdraw(saleId, quantity)) {
            // Só o caminho de falha paga a consulta extra para diferenciar "não existe" de
            // "esgotou".
            if (!saleRepository.existsById(saleId)) {
                throw new SaleNotFoundException(saleId);
            }
            throw new SoldOutException(saleId, quantity);
        }

        Reservation reservation =
                Reservation.hold(
                        saleId, customerId, quantity, clock.instant(), properties.holdDuration());
        reservationRepository.save(reservation);
        eventPublisher.publish(reservation.pullDomainEvents());
        return reservation;
    }

    @Transactional(readOnly = true)
    public Reservation get(ReservationId id) {
        return reservationRepository
                .findById(id)
                .orElseThrow(() -> new ReservationNotFoundException(id));
    }
}
