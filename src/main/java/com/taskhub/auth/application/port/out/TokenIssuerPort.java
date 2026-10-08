package com.taskhub.auth.application.port.out;

import com.taskhub.auth.domain.model.Role;

public interface TokenIssuerPort {

    String issue(Long userId, String email, Role role);
}
