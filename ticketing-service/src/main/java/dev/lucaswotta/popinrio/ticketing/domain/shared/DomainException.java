package dev.lucaswotta.popinrio.ticketing.domain.shared;

/**
 * Violação de regra de negócio. A borda web traduz cada subclasse em um status HTTP; o domínio não
 * sabe que HTTP existe.
 */
public abstract class DomainException extends RuntimeException {

    protected DomainException(String message) {
        super(message);
    }
}
