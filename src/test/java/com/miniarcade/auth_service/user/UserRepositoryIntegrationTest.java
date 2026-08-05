package com.miniarcade.auth_service.user;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class UserRepositoryIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Autowired
    private UserRepository userRepository;

    @Test
    void savesAndFindsUserByUsername() {
        User user = User.builder()
                .username("integration-user")
                .email("integration@example.com")
                .password("hashed-password")
                .build();

        userRepository.save(user);

        Optional<User> found = userRepository.findByUsername("integration-user");

        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("integration@example.com");
        assertThat(found.get().getRole()).isEqualTo(Role.USER);
    }

    @Test
    void existsByUsernameAndEmailReflectPersistedState() {
        User user = User.builder()
                .username("another-user")
                .email("another@example.com")
                .password("hashed-password")
                .build();

        userRepository.save(user);

        assertThat(userRepository.existsByUsername("another-user")).isTrue();
        assertThat(userRepository.existsByEmail("another@example.com")).isTrue();
        assertThat(userRepository.existsByUsername("nonexistent")).isFalse();
    }
}
