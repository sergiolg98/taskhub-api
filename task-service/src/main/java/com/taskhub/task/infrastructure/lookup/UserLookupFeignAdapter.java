package com.taskhub.task.infrastructure.lookup;

import com.taskhub.task.application.port.out.UserLookupPort;
import com.taskhub.task.application.port.out.UserLookupResult;
import com.taskhub.task.domain.model.UserSummary;
import feign.FeignException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

// Replaces the local adapter of the monolith. The port and the domain did not change (the result type became explicit in class 11).
//
// Aspect order (fixed by Resilience4j): Retry( CircuitBreaker( call ) ). The fallback is declared on @Retry, the OUTERMOST aspect:
// if it were on @CircuitBreaker, it would swallow the exception and the retry would never see a failure.
// Both annotations work because the port method is called from another bean, through the Spring proxy (a this.method() call would skip them).
@Component
class UserLookupFeignAdapter implements UserLookupPort {

    private static final Logger log = LoggerFactory.getLogger(UserLookupFeignAdapter.class);

    private final AuthClient authClient;

    UserLookupFeignAdapter(AuthClient authClient) {
        this.authClient = authClient;
    }

    // A GET is idempotent, so retrying it is safe. Never put @Retry on a POST that creates something.
    @Override
    @Retry(name = "authService", fallbackMethod = "unavailable")
    @CircuitBreaker(name = "authService")
    public UserLookupResult findById(Long userId) {
        try {
            UserSummaryResponse response = authClient.findById(userId);
            return new UserLookupResult.Found(new UserSummary(response.id(), response.name(), response.role()));
        } catch (AuthClientConfig.AuthUserNotFoundException e) {
            return new UserLookupResult.NotFound();
        } catch (FeignException e) {
            // Connection refused, timeout, broken connection or undecodable body: we cannot tell if the user exists.
            throw new ExternalServiceException("auth-service is not reachable", e);
        }
    }

    // Reached when the retries are exhausted, the circuit is open, or auth-service rejected the request.
    // Not an invented user: it only says "I could not check". The caller decides what that means for the business.
    UserLookupResult unavailable(Long userId, Throwable cause) {
        log.warn("auth-service lookup unavailable for user {}: {}", userId, cause.toString());
        return new UserLookupResult.Unavailable(cause.getClass().getSimpleName());
    }
}
