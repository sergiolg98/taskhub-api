package com.taskhub.task.infrastructure.lookup;

import feign.RequestInterceptor;
import feign.Response;
import feign.codec.ErrorDecoder;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

// Deliberately NOT annotated with @Configuration: it applies only to AuthClient (Feign creates a child context for it).
class AuthClientConfig {

    /** auth-service answered 404: the user does not exist. Internal signal between the decoder and the adapter. */
    static class AuthUserNotFoundException extends RuntimeException {
    }

    // The call is made on behalf of the user: the caller's token travels with it. A service-to-service credential
    // would be the alternative (not implemented). X-Request-Id keeps the correlation id across the hop.
    @Bean
    RequestInterceptor forwardCallerHeaders() {
        return template -> {
            if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
                for (String header : new String[]{HttpHeaders.AUTHORIZATION, "X-Request-Id"}) {
                    String value = attributes.getRequest().getHeader(header);
                    if (value != null) {
                        template.header(header, value);
                    }
                }
            }
        };
    }

    // Does not read the body of the error: it may not be JSON (a proxy page, an empty 502...).
    @Bean
    ErrorDecoder authErrorDecoder() {
        return (String methodKey, Response response) -> {
            int status = response.status();
            if (status == 404) {
                return new AuthUserNotFoundException();        // a business answer, not a failure
            }
            return status >= 500
                    ? new ExternalServiceException("auth-service answered " + status)   // a failure: counts and retries
                    : new AuthRejectedException(status);                                // alive but refusing: neither
        };
    }
}
