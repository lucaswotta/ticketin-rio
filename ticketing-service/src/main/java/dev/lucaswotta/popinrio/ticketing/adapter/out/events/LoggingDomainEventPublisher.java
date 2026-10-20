package dev.lucaswotta.popinrio.ticketing.adapter.out.events;

import dev.lucaswotta.popinrio.ticketing.application.DomainEventPublisher;
import dev.lucaswotta.popinrio.ticketing.domain.shared.DomainEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Implementação provisória: registra os eventos no log. Na fase de mensageria, é substituída por
 * uma que grava na tabela de outbox, sem mudar nenhuma linha do caso de uso. Essa troca é o motivo
 * de a porta existir.
 */
@Component
class LoggingDomainEventPublisher implements DomainEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(LoggingDomainEventPublisher.class);

    @Override
    public void publish(List<DomainEvent> events) {
        events.forEach(
                event ->
                        log.info(
                                "domain_event type={} payload={}",
                                event.getClass().getSimpleName(),
                                event));
    }
}
