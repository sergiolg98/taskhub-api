package com.taskhub.common.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

// This service only validates tokens. The tests sign them by hand, the way auth-service does (claims uid and role).
class JwtParserTest {

    private static final String SECRET = "a-test-secret-with-more-than-thirty-two-bytes";

    private final JwtParser parser = new JwtParser(SECRET);

    @Test
    void validTokenYieldsThePrincipalFromItsClaimsOnly() {
        String token = sign(SECRET, 2L, "luis@taskhub.com", "USER");

        assertThat(parser.parse(token)).contains(new JwtPrincipal(2L, "luis@taskhub.com", "USER"));
    }

    @Test
    void adminRoleIsRecognised() {
        String token = sign(SECRET, 1L, "ana@taskhub.com", "ADMIN");

        assertThat(parser.parse(token)).get().extracting(JwtPrincipal::isAdmin).isEqualTo(true);
    }

    @Test
    void tamperedTokenIsRejected() {
        assertThat(parser.parse(sign(SECRET, 2L, "luis@taskhub.com", "USER") + "x")).isEmpty();
    }

    @Test
    void tokenSignedWithAnotherSecretIsRejected() {
        String token = sign("another-secret-with-more-than-thirty-two-bytes", 2L, "luis@taskhub.com", "USER");

        assertThat(parser.parse(token)).isEmpty();
    }

    @Test
    void expiredTokenIsRejected() {
        String token = Jwts.builder().subject("luis@taskhub.com").claim("uid", 2L).claim("role", "USER")
                .expiration(new Date(System.currentTimeMillis() - 1000))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)), Jwts.SIG.HS256)
                .compact();

        assertThat(parser.parse(token)).isEmpty();
    }

    @Test
    void tokenWithoutTheUserIdClaimIsRejected() {
        String token = Jwts.builder().subject("luis@taskhub.com").claim("role", "USER")
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)), Jwts.SIG.HS256)
                .compact();

        assertThat(parser.parse(token)).isEmpty();
    }

    @Test
    void garbageIsRejected() {
        assertThat(parser.parse("not-a-jwt")).isEmpty();
    }

    private static String sign(String secret, Long uid, String email, String role) {
        return Jwts.builder().subject(email).claim("uid", uid).claim("role", role)
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)), Jwts.SIG.HS256)
                .compact();
    }
}
