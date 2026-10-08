package com.taskhub.gateway;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Correlation id: every request leaves the gateway with an X-Request-Id, both towards the service and back to the client,
 * and one log line per request ties the id to the outcome.
 */
@Component
class RequestIdFilter implements GlobalFilter, Ordered {

    static final String HEADER = "X-Request-Id";

    private static final Logger log = LoggerFactory.getLogger(RequestIdFilter.class);
    // A client-supplied id is reused only if it is harmless (it ends up in logs); otherwise we generate our own.
    private static final Pattern SAFE_ID = Pattern.compile("[A-Za-z0-9_-]{1,64}");

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String incoming = exchange.getRequest().getHeaders().getFirst(HEADER);
        String id = incoming != null && SAFE_ID.matcher(incoming).matches() ? incoming : UUID.randomUUID().toString();

        exchange.getResponse().getHeaders().set(HEADER, id);
        var request = exchange.getRequest().mutate().header(HEADER, id).build();
        long start = System.nanoTime();

        return chain.filter(exchange.mutate().request(request).build()).doFinally(signal -> log.info(
                "{} {} -> {} in {} ms [{}]", request.getMethod(), request.getURI().getPath(),
                exchange.getResponse().getStatusCode(), (System.nanoTime() - start) / 1_000_000, id));
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
