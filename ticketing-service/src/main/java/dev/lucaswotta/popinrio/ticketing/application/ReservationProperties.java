package dev.lucaswotta.popinrio.ticketing.application;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/** Por quanto tempo os ingressos ficam segurados antes de voltarem ao estoque. */
@ConfigurationProperties(prefix = "pop-in-rio.reservation")
public record ReservationProperties(Duration holdDuration) {

    public ReservationProperties {
        if (holdDuration == null || holdDuration.isNegative() || holdDuration.isZero()) {
            throw new IllegalArgumentException(
                    "pop-in-rio.reservation.hold-duration deve ser positiva");
        }
    }
}
