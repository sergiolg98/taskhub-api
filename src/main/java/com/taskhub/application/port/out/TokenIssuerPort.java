package com.taskhub.application.port.out;

import com.taskhub.domain.model.Role;

public interface TokenIssuerPort {

    String issue(String email, Role role);
}
