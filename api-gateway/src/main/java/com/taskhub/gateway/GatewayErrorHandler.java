package com.taskhub.gateway;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.ConnectException;
import java.util.concurrent.TimeoutException;

// Turns any failure inside the gateway (route not found, service down, timeout) into the shared error format.
// Order -2 puts it before Spring Boot's default handler (-1).
@Order(-2)
@Component
class GatewayErrorHandler implements ErrorWebExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GatewayErrorHandler.class);

    private final ErrorBodies errors;

    GatewayErrorHandler(ErrorBodies errors) {
        this.errors = errors;
    }

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        HttpStatus status = statusOf(ex);
        if (status.is5xxServerError()) {
            log.warn("{} {} failed: {}", exchange.getRequest().getMethod(), exchange.getRequest().getURI().getPath(), ex.toString());
        }
        return errors.write(exchange, status, status == HttpStatus.INTERNAL_SERVER_ERROR
                ? "Internal server error" : status.getReasonPhrase());
    }

    private static HttpStatus statusOf(Throwable ex) {
        for (Throwable t = ex; t != null; t = t.getCause()) {
            if (t instanceof ConnectException) {
                return HttpStatus.SERVICE_UNAVAILABLE;
            }
            if (t instanceof TimeoutException) {
                return HttpStatus.GATEWAY_TIMEOUT;
            }
            if (t instanceof ResponseStatusException rse) {
                HttpStatus status = HttpStatus.resolve(rse.getStatusCode().value());
                if (status != null) {
                    return status;
                }
            }
        }
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }
}
