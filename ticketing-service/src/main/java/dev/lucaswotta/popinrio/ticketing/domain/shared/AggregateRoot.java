package dev.lucaswotta.popinrio.ticketing.domain.shared;

import java.util.ArrayList;
import java.util.List;

/**
 * Raiz de agregado: a única porta de entrada para alterar o agregado, e quem acumula os eventos de
 * domínio gerados por essas alterações.
 *
 * <p>Os eventos ficam guardados até a camada de aplicação recolhê-los com {@link
 * #pullDomainEvents()}, depois de persistir o agregado. Na fase de mensageria, eles vão para a
 * tabela de outbox na mesma transação.
 */
public abstract class AggregateRoot {

    private final List<DomainEvent> domainEvents = new ArrayList<>();

    protected void registerEvent(DomainEvent event) {
        domainEvents.add(event);
    }

    public List<DomainEvent> pullDomainEvents() {
        List<DomainEvent> pulled = List.copyOf(domainEvents);
        domainEvents.clear();
        return pulled;
    }
}
