package com.taskhub.task.infrastructure.lookup;

import com.taskhub.task.application.port.out.UserLookupPort;
import com.taskhub.task.domain.exception.ExternalServiceException;
import com.taskhub.task.domain.model.UserSummary;
import feign.FeignException;
import org.springframework.stereotype.Component;

import java.util.Optional;

// Replaces the local adapter of the monolith. The port, the domain and the use cases did not change.
@Component
class UserLookupFeignAdapter implements UserLookupPort {

    private final AuthClient authClient;

    UserLookupFeignAdapter(AuthClient authClient) {
        this.authClient = authClient;
    }

    @Override
    public Optional<UserSummary> findById(Long userId) {
        try {
            UserSummaryResponse response = authClient.findById(userId);
            return Optional.of(new UserSummary(response.id(), response.name(), response.role()));
        } catch (AuthClientConfig.AuthUserNotFoundException e) {
            return Optional.empty();
        } catch (FeignException e) {
            // Connection refused, timeout, broken connection or undecodable body: we cannot tell if the user exists.
            throw new ExternalServiceException("auth-service is not reachable", e);
        }
    }
}
