package com.taskhub.infrastructure.security;

import com.taskhub.application.port.out.UserRepositoryPort;
import com.taskhub.domain.exception.EmailAlreadyUsedException;
import com.taskhub.domain.model.User;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthenticationService {

    private final UserRepositoryPort users;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthenticationService(UserRepositoryPort users, PasswordEncoder passwordEncoder,
                                 AuthenticationManager authenticationManager, JwtService jwtService) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    public String register(String name, String email, String rawPassword) {
        if (users.existsByEmail(email)) {
            throw new EmailAlreadyUsedException(email);
        }
        User saved = users.save(User.register(name, email, passwordEncoder.encode(rawPassword)));
        return jwtService.generateToken(SecurityUser.toSecurityUserFromDomain(saved));
    }

    public String login(String email, String rawPassword) {
        var auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, rawPassword));
        return jwtService.generateToken((UserDetails) auth.getPrincipal());
    }
}
