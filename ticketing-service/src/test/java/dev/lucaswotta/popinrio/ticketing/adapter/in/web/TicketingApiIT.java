package dev.lucaswotta.popinrio.ticketing.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;

import dev.lucaswotta.popinrio.ticketing.support.IntegrationTest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;

import java.util.Map;
import java.util.UUID;

/** Exercita a API por HTTP de verdade, do JSON de entrada ao Problem Details de saída. */
class TicketingApiIT extends IntegrationTest {

    @Value("${local.server.port}")
    private int port;

    private RestClient http;

    @BeforeEach
    void setUp() {
        http =
                RestClient.builder()
                        .baseUrl("http://localhost:" + port)
                        // Erros 4xx são resultado esperado aqui, não exceção.
                        .defaultStatusHandler(status -> true, (request, response) -> {})
                        .build();
    }

    @Test
    void openSaleHoldTicketsAndReadBothBack() {
        ResponseEntity<Map> sale =
                post("/api/sales", Map.of("name", "Pop in Rio", "totalTickets", 10));
        assertThat(sale.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(sale.getHeaders().getLocation()).isNotNull();
        String saleId = (String) sale.getBody().get("id");

        ResponseEntity<Map> reservation =
                post(
                        "/api/sales/" + saleId + "/reservations",
                        Map.of("customerId", "lucas", "quantity", 3));
        assertThat(reservation.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(reservation.getBody()).containsEntry("status", "HELD");
        String reservationId = (String) reservation.getBody().get("id");

        ResponseEntity<Map> fetchedReservation = get("/api/reservations/" + reservationId);
        assertThat(fetchedReservation.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(fetchedReservation.getBody()).containsEntry("quantity", 3);

        ResponseEntity<Map> fetchedSale = get("/api/sales/" + saleId);
        assertThat(fetchedSale.getBody())
                .containsEntry("availableTickets", 7)
                .containsEntry("soldTickets", 3);
    }

    @Test
    void soldOutIsConflictWithStableCode() {
        String saleId = openSale(2);

        ResponseEntity<Map> response =
                post(
                        "/api/sales/" + saleId + "/reservations",
                        Map.of("customerId", "lucas", "quantity", 3));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).containsEntry("code", "SOLD_OUT");
    }

    @Test
    void quantityAboveLimitIsUnprocessable() {
        String saleId = openSale(10);

        ResponseEntity<Map> response =
                post(
                        "/api/sales/" + saleId + "/reservations",
                        Map.of("customerId", "lucas", "quantity", 5));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
        assertThat(response.getBody()).containsEntry("code", "BUSINESS_RULE_VIOLATION");
    }

    @Test
    void unknownSaleIsNotFound() {
        ResponseEntity<Map> response =
                post(
                        "/api/sales/" + UUID.randomUUID() + "/reservations",
                        Map.of("customerId", "lucas", "quantity", 1));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).containsEntry("code", "NOT_FOUND");
    }

    @Test
    void malformedRequestsAreBadRequest() {
        String saleId = openSale(10);

        assertThat(
                        post("/api/sales/" + saleId + "/reservations", Map.of("quantity", 1))
                                .getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(get("/api/sales/not-a-uuid").getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    private String openSale(int tickets) {
        return (String)
                post("/api/sales", Map.of("name", "Pop in Rio", "totalTickets", tickets))
                        .getBody()
                        .get("id");
    }

    @SuppressWarnings("rawtypes")
    private ResponseEntity<Map> post(String path, Map<String, Object> body) {
        return http.post()
                .uri(path)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toEntity(Map.class);
    }

    @SuppressWarnings("rawtypes")
    private ResponseEntity<Map> get(String path) {
        return http.get().uri(path).retrieve().toEntity(Map.class);
    }
}
