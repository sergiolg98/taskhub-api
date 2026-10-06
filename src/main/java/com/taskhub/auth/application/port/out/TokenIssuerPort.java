package com.taskhub.auth.application.port.out;

import com.taskhub.auth.domain.model.Role;

public interface TokenIssuerPort {

    String issue(String email, Role role);
}
