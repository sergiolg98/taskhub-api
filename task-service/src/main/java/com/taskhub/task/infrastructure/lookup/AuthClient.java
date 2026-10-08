package com.taskhub.task.infrastructure.lookup;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

// Declarative HTTP client. It lives in infrastructure, behind the UserLookupPort: the domain never hears about Feign.
// No service discovery: the URL comes from the configuration (config-repo).
@FeignClient(name = "auth-service", url = "${taskhub.auth-service.url}", configuration = AuthClientConfig.class)
interface AuthClient {

    @GetMapping("/users/{id}")
    UserSummaryResponse findById(@PathVariable("id") Long id);
}
