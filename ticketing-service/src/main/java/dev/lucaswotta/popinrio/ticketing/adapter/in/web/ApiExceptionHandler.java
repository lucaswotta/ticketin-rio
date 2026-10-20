package dev.lucaswotta.popinrio.ticketing.adapter.in.web;

import dev.lucaswotta.popinrio.ticketing.domain.reservation.InvalidReservationTransitionException;
import dev.lucaswotta.popinrio.ticketing.domain.reservation.ReservationNotFoundException;
import dev.lucaswotta.popinrio.ticketing.domain.sale.SaleNotFoundException;
import dev.lucaswotta.popinrio.ticketing.domain.sale.SoldOutException;
import dev.lucaswotta.popinrio.ticketing.domain.shared.DomainException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Traduz exceções de domínio em respostas RFC 9457 (Problem Details). Erros de formato da
 * requisição (JSON inválido, campo faltando, UUID malformado) já são tratados pela classe base com
 * 400.
 *
 * <p>O campo {@code code} é estável e pensado para o front-end decidir o que mostrar sem depender
 * do texto da mensagem.
 */
@RestControllerAdvice
class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler({SaleNotFoundException.class, ReservationNotFoundException.class})
    ProblemDetail notFound(DomainException e) {
        return problem(HttpStatus.NOT_FOUND, "NOT_FOUND", e);
    }

    @ExceptionHandler(SoldOutException.class)
    ProblemDetail soldOut(SoldOutException e) {
        return problem(HttpStatus.CONFLICT, "SOLD_OUT", e);
    }

    @ExceptionHandler(InvalidReservationTransitionException.class)
    ProblemDetail invalidTransition(InvalidReservationTransitionException e) {
        return problem(HttpStatus.CONFLICT, "INVALID_RESERVATION_STATE", e);
    }

    /** Qualquer outra regra de negócio violada. */
    @ExceptionHandler(DomainException.class)
    ProblemDetail businessRule(DomainException e) {
        return problem(HttpStatus.UNPROCESSABLE_CONTENT, "BUSINESS_RULE_VIOLATION", e);
    }

    private static ProblemDetail problem(HttpStatus status, String code, DomainException e) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, e.getMessage());
        problem.setProperty("code", code);
        return problem;
    }
}
