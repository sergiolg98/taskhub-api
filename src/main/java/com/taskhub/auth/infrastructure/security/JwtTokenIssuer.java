package com.taskhub.auth.infrastructure.security;

import com.taskhub.auth.application.port.out.TokenIssuerPort;
import com.taskhub.auth.domain.model.Role;
import com.taskhub.common.security.JwtService;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenIssuer implements TokenIssuerPort {

    private final JwtService jwtService;

    public JwtTokenIssuer(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public String issue(String email, Role role) {
        return jwtService.generateToken(email, role);
    }
}
