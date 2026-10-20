package dev.lucaswotta.popinrio.ticketing.domain.reservation;

public enum ReservationStatus {
    /** Ingressos já retirados do estoque, segurados para o comprador até o prazo vencer. */
    HELD,
    CONFIRMED,
    EXPIRED,
    CANCELLED;

    public boolean isFinal() {
        return this != HELD;
    }
}
