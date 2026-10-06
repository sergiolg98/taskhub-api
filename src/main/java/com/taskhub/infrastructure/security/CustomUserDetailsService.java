package com.taskhub.infrastructure.security;

import com.taskhub.application.port.out.UserRepositoryPort;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {
    private final UserRepositoryPort users;

    public CustomUserDetailsService(UserRepositoryPort users) {
        this.users = users;
    }

    @Override
    public UserDetails loadUserByUsername(String email) {
        return users.findByEmail(email)
                .map(SecurityUser::toSecurityUserFromDomain)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));
    }
}
