package dev.lucaswotta.popinrio.ticketing.domain.sale;

/**
 * Quantidade de ingressos em uma operação. Objeto de valor: sempre válido depois de construído,
 * então nenhum método que recebe um {@code TicketQuantity} precisa revalidar se é positivo.
 */
public record TicketQuantity(int value) {

    public TicketQuantity {
        if (value < 1) {
            throw new InvalidQuantityException("A quantidade de ingressos deve ser maior que zero");
        }
    }

    public static TicketQuantity of(int value) {
        return new TicketQuantity(value);
    }

    public boolean isGreaterThan(int other) {
        return value > other;
    }
}
