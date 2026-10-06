package com.taskhub.task.application.port.out;

import com.taskhub.task.domain.model.UserSummary;

/**
 * The three possible answers to "who is this user?". Making "Unavailable" explicit matters: an empty Optional
 * would mix "the user does not exist" with "I could not ask", and those need different reactions.
 */
public sealed interface UserLookupResult {

    record Found(UserSummary user) implements UserLookupResult {
    }

    /** The user definitely does not exist. */
    record NotFound() implements UserLookupResult {
    }

    /** No trustworthy answer (service down, slow, failing, or circuit open). Says nothing about the user. */
    record Unavailable(String reason) implements UserLookupResult {
    }
}
