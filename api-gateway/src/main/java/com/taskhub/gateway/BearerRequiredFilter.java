package com.taskhub.gateway;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Security model A: the gateway only routes; each service validates the JWT (signature, expiry, roles, ownership).
 * The one thing the gateway adds is a cheap early rejection: outside the public paths a request without
 * "Authorization: Bearer <something>" never reaches a service. It does NOT parse or trust the token.
 */
@Component
class BearerRequiredFilter implements GlobalFilter, Ordered {

    private final List<PathPattern> publicPaths;
    private final ErrorBodies errors;

    BearerRequiredFilter(@Value("${taskhub.gateway.public-paths:/auth/**}") List<String> publicPaths, ErrorBodies errors) {
        this.publicPaths = publicPaths.stream().map(PathPatternParser.defaultInstance::parse).toList();
        this.errors = errors;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        var request = exchange.getRequest();
        boolean isPublic = publicPaths.stream().anyMatch(p -> p.matches(request.getPath().pathWithinApplication()));
        if (isPublic || request.getMethod() == HttpMethod.OPTIONS || hasBearer(request.getHeaders())) {
            return chain.filter(exchange);
        }
        return errors.write(exchange, HttpStatus.UNAUTHORIZED, "Unauthorized");
    }

    private static boolean hasBearer(HttpHeaders headers) {
        String value = headers.getFirst(HttpHeaders.AUTHORIZATION);
        return value != null && value.startsWith("Bearer ") && value.length() > "Bearer ".length();
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 10;
    }
}
