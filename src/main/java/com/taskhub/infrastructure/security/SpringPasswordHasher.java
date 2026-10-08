package com.taskhub.infrastructure.security;

import com.taskhub.application.port.out.PasswordHasherPort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class SpringPasswordHasher implements PasswordHasherPort {

    private final PasswordEncoder passwordEncoder;

    public SpringPasswordHasher(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public String hash(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }
}
