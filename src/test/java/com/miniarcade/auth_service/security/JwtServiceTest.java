package com.miniarcade.auth_service.security;

import com.miniarcade.auth_service.user.Role;
import com.miniarcade.auth_service.user.User;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private static final String TEST_SECRET =
            "test-secret-key-at-least-64-bytes-long-for-hs512-xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx";

    private final User user = User.builder()
            .id(42L)
            .username("alice")
            .email("alice@example.com")
            .password("irrelevant")
            .role(Role.USER)
            .build();

    @Test
    void generateToken_encodesUsernameAndRole_extractableFromToken() {
        JwtService jwtService = new JwtService(TEST_SECRET, 60_000L);

        String token = jwtService.generateToken(user);

        assertThat(jwtService.extractUsername(token)).isEqualTo("alice");
        assertThat(jwtService.extractRole(token)).isEqualTo("USER");
    }

    @Test
    void isTokenValid_returnsTrue_forFreshlyGeneratedToken() {
        JwtService jwtService = new JwtService(TEST_SECRET, 60_000L);

        String token = jwtService.generateToken(user);

        assertThat(jwtService.isTokenValid(token)).isTrue();
    }

    @Test
    void isTokenValid_returnsFalse_forGarbageToken() {
        JwtService jwtService = new JwtService(TEST_SECRET, 60_000L);

        assertThat(jwtService.isTokenValid("not-a-real-jwt")).isFalse();
    }

    @Test
    void isTokenValid_returnsFalse_forExpiredToken() throws InterruptedException {
        JwtService jwtService = new JwtService(TEST_SECRET, 1L);

        String token = jwtService.generateToken(user);
        Thread.sleep(10);

        assertThat(jwtService.isTokenValid(token)).isFalse();
    }

    @Test
    void isTokenValid_returnsFalse_whenSignedWithDifferentSecret() {
        JwtService issuer = new JwtService(TEST_SECRET, 60_000L);
        JwtService verifier = new JwtService(
                "a-completely-different-secret-key-that-is-also-at-least-64-bytes-long-xx",
                60_000L
        );

        String token = issuer.generateToken(user);

        assertThat(verifier.isTokenValid(token)).isFalse();
    }
}
