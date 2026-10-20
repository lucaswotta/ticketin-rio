package dev.lucaswotta.popinrio.ticketing.domain.reservation;

/**
 * Identificador do comprador. No contexto de Ingressos, o comprador é só uma referência: quem ele é
 * pertence ao contexto de Identidade.
 */
public record CustomerId(String value) {

    public static final int MAX_LENGTH = 60;

    public CustomerId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("CustomerId é obrigatório");
        }
        value = value.strip();
        if (value.length() > MAX_LENGTH) {
            throw new IllegalArgumentException(
                    "CustomerId deve ter no máximo " + MAX_LENGTH + " caracteres");
        }
    }

    @Override
    public String toString() {
        return value;
    }
}
