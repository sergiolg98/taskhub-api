package com.taskhub.gateway;

import io.netty.resolver.DefaultAddressResolverGroup;
import org.springframework.cloud.gateway.config.HttpClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Service names are resolved with the JDK resolver, whose cache TTL we control (see ApiGatewayApplication).
 *
 * Why: Reactor Netty's own DNS resolver honours the TTL that Docker's DNS returns (minutes). After a container is
 * restarted and gets another IP, the gateway kept calling the old address and answered 503 until it was restarted too
 * (found while running the integration checklist in class 14).
 */
@Configuration
class DnsConfig {

    @Bean
    HttpClientCustomizer jdkDnsResolver() {
        return httpClient -> httpClient.resolver(DefaultAddressResolverGroup.INSTANCE);
    }
}
