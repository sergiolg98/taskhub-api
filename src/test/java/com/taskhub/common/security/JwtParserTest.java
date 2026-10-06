package com.taskhub.common.security;

import com.taskhub.auth.domain.model.Role;
import com.taskhub.auth.infrastructure.security.JwtTokenIssuer;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

// Issuer and parser are separate on purpose: a service that only validates tokens never needs the issuer.
class JwtParserTest {

    private static final String SECRET = "a-test-secret-with-more-than-thirty-two-bytes";

    private final JwtParser parser = new JwtParser(SECRET);
    private final JwtTokenIssuer issuer = new JwtTokenIssuer(SECRET, 5);

    @Test
    void validTokenYieldsThePrincipalFromItsClaimsOnly() {
        String token = issuer.issue(2L, "luis@taskhub.com", Role.USER);

        assertThat(parser.parse(token)).contains(new JwtPrincipal(2L, "luis@taskhub.com", "USER"));
    }

    @Test
    void adminRoleIsRecognised() {
        String token = issuer.issue(1L, "ana@taskhub.com", Role.ADMIN);

        assertThat(parser.parse(token)).get().extracting(JwtPrincipal::isAdmin).isEqualTo(true);
    }

    @Test
    void tamperedTokenIsRejected() {
        assertThat(parser.parse(issuer.issue(2L, "luis@taskhub.com", Role.USER) + "x")).isEmpty();
    }

    @Test
    void tokenSignedWithAnotherSecretIsRejected() {
        String token = new JwtTokenIssuer("another-secret-with-more-than-thirty-two-bytes", 5)
                .issue(2L, "luis@taskhub.com", Role.USER);

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
}
