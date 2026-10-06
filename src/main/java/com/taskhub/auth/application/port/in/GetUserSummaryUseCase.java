package com.taskhub.auth.application.port.in;

import com.taskhub.auth.domain.model.UserSummary;

public interface GetUserSummaryUseCase {

    /** @throws com.taskhub.auth.domain.exception.UserNotFoundException if there is no such user */
    UserSummary getSummary(Long id);
}
