package com.taskhub.auth.infrastructure.security;

import com.taskhub.auth.domain.model.AuthenticatedUser;
import com.taskhub.auth.domain.model.Role;
import com.taskhub.auth.domain.model.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public class SecurityUser implements UserDetails {

    private final Long id;
    private final String email;
    private final String password;
    private final Role role;

    public SecurityUser(Long id, String email, String password, Role role) {
        this.id = id;
        this.email = email;
        this.password = password;
        this.role = role;
    }

    // mappers
    public static SecurityUser toSecurityUserFromDomain(User user) {
        return new SecurityUser(user.getId(), user.getEmail(), user.getPassword(), user.getRole());
    }

    public AuthenticatedUser toAuthenticatedUser() {
        return new AuthenticatedUser(id, role);
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() { return password; }

    @Override
    public String getUsername() { return email; }
}
