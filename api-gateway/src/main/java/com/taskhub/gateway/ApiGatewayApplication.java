package com.taskhub.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ApiGatewayApplication {

    public static void main(String[] args) {
        // Seconds the JVM caches a resolved service name. Must be set before the first lookup. Short, so a restarted
        // container with a new IP is found again quickly (see DnsConfig).
        java.security.Security.setProperty("networkaddress.cache.ttl", "10");
        SpringApplication.run(ApiGatewayApplication.class, args);
    }
}
