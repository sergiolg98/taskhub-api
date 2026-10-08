package com.taskhub.task.infrastructure.lookup;

import com.taskhub.auth.application.port.in.GetUserSummaryUseCase;
import com.taskhub.auth.domain.exception.UserNotFoundException;
import com.taskhub.task.application.port.out.UserLookupPort;
import com.taskhub.task.domain.model.UserSummary;
import org.springframework.stereotype.Component;

import java.util.Optional;

// The ONLY class of the task area allowed to know the auth area (ArchitectureTest enforces it).
// It translates auth's contract into task's own model. When auth becomes a separate service,
// this adapter is replaced by one that calls HTTP and nothing else in the task area changes.
@Component
class UserLookupLocalAdapter implements UserLookupPort {

    private final GetUserSummaryUseCase getUserSummary;

    UserLookupLocalAdapter(GetUserSummaryUseCase getUserSummary) {
        this.getUserSummary = getUserSummary;
    }

    @Override
    public Optional<UserSummary> findById(Long userId) {
        try {
            var summary = getUserSummary.getSummary(userId);
            return Optional.of(new UserSummary(summary.id(), summary.name(), summary.role().name()));
        } catch (UserNotFoundException e) {
            return Optional.empty();
        }
    }
}
