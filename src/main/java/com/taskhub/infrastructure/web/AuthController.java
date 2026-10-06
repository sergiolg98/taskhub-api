package com.taskhub.infrastructure.web;

import com.taskhub.infrastructure.security.AuthenticationService;
import com.taskhub.infrastructure.web.dto.AuthResponse;
import com.taskhub.infrastructure.web.dto.LoginRequest;
import com.taskhub.infrastructure.web.dto.RegisterRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthenticationService authenticationService;

    public AuthController(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return new AuthResponse(authenticationService.register(request.name(), request.email(), request.password()));
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return new AuthResponse(authenticationService.login(request.email(), request.password()));
    }
}
