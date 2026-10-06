package com.taskhub.gateway;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

// The gateway answers with the same ErrorResponse shape as the services (status, message, details, timestamp),
// so a client parses one error format no matter who produced the error.
@Component
class ErrorBodies {

    record ErrorResponse(int status, String message, List<String> details, LocalDateTime timestamp) {
    }

    private final ObjectMapper mapper;

    ErrorBodies(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    Mono<Void> write(ServerWebExchange exchange, HttpStatus status, String message) {
        var response = exchange.getResponse();
        if (response.isCommitted()) {
            return Mono.empty();
        }
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        byte[] json;
        try {
            json = mapper.writeValueAsBytes(new ErrorResponse(status.value(), message, List.of(), LocalDateTime.now()));
        } catch (JsonProcessingException e) {
            json = ("{\"status\":" + status.value() + "}").getBytes(StandardCharsets.UTF_8);
        }
        DataBuffer buffer = response.bufferFactory().wrap(json);
        return response.writeWith(Mono.just(buffer));
    }
}
