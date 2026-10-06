package com.taskhub;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import java.nio.charset.StandardCharsets;
import java.util.Date;

// task-service has no login: tests sign the tokens the way auth-service would (same secret, claims uid and role).
final class TestTokens {

    private static final String SECRET = "test-secret-for-jwt-tests-must-be-at-least-32-bytes-long";

    private TestTokens() {
    }

    static String token(Long userId, String email, String role) {
        return "Bearer " + Jwts.builder().subject(email).claim("uid", userId).claim("role", role)
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)), Jwts.SIG.HS256)
                .compact();
    }

    static String luis() {
        return token(2L, "luis@taskhub.com", "USER");
    }

    static String eva() {
        return token(3L, "eva@taskhub.com", "USER");
    }

    static String ana() {
        return token(1L, "ana@taskhub.com", "ADMIN");
    }
}
