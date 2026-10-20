package dev.lucaswotta.popinrio.ticketing.application;

import dev.lucaswotta.popinrio.ticketing.domain.shared.DomainEvent;

import java.util.List;

/**
 * Porta de saída para eventos de domínio. Precisa ser chamada dentro da transação do caso de uso:
 * na fase de mensageria, a implementação grava os eventos na tabela de outbox, e é a transação que
 * garante "estado salvo se e somente se evento registrado".
 */
public interface DomainEventPublisher {

    void publish(List<DomainEvent> events);
}
