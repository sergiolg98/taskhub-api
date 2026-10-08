package com.taskhub.task.application.port.out;

/** Asks "who is this user?" without knowing where the answer comes from, and without throwing for expected failures. */
public interface UserLookupPort {

    UserLookupResult findById(Long userId);
}
