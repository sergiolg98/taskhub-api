package com.taskhub.common.web;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void unexpectedErrorsAreAGenericFiveHundredThatDoesNotLeakTheCause() throws Exception {
        var response = handler.handleUnexpected(new IllegalStateException("secret internal detail"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().message()).isEqualTo("Internal server error");
    }

    @Test
    void illegalArgumentIsNotTreatedAsAClientError() throws Exception {
        var response = handler.handleUnexpected(new IllegalArgumentException("bug"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    void securityExceptionsAreLeftToSpringSecurity() {
        assertThatThrownBy(() -> handler.handleUnexpected(new AccessDeniedException("no")))
                .isInstanceOf(AccessDeniedException.class);
    }
}
