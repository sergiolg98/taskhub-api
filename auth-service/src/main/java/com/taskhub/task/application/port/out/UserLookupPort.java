package com.taskhub.task.application.port.out;

import com.taskhub.task.domain.model.UserSummary;

import java.util.Optional;

/**
 * Asks "who is this user?" without knowing where the answer comes from.
 * Today it is answered inside the monolith; later by another service over HTTP.
 */
public interface UserLookupPort {

    Optional<UserSummary> findById(Long userId);
}
