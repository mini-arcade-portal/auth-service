package com.miniarcade.auth_service.auth;

import com.miniarcade.auth_service.auth.dto.AuthResponse;
import com.miniarcade.auth_service.auth.dto.LoginRequest;
import com.miniarcade.auth_service.auth.dto.RegisterRequest;
import com.miniarcade.auth_service.exception.EmailAlreadyExistsException;
import com.miniarcade.auth_service.exception.InvalidCredentialsException;
import com.miniarcade.auth_service.exception.UsernameAlreadyExistsException;
import com.miniarcade.auth_service.security.JwtService;
import com.miniarcade.auth_service.user.Role;
import com.miniarcade.auth_service.user.User;
import com.miniarcade.auth_service.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, passwordEncoder, jwtService);
    }

    @Test
    void register_savesUserWithEncodedPasswordAndUserRole_andReturnsToken() {
        RegisterRequest request = new RegisterRequest("newuser", "newuser@example.com", "plainPassword");

        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("newuser@example.com")).thenReturn(false);
        when(passwordEncoder.encode("plainPassword")).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(1L);
            return u;
        });
        when(jwtService.generateToken(any(User.class))).thenReturn("generated-token");

        AuthResponse response = authService.register(request);

        ArgumentCaptor<User> savedUserCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(savedUserCaptor.capture());
        User savedUser = savedUserCaptor.getValue();

        assertThat(savedUser.getUsername()).isEqualTo("newuser");
        assertThat(savedUser.getEmail()).isEqualTo("newuser@example.com");
        assertThat(savedUser.getPassword()).isEqualTo("encodedPassword");
        assertThat(savedUser.getRole()).isEqualTo(Role.USER);

        assertThat(response.token()).isEqualTo("generated-token");
        assertThat(response.username()).isEqualTo("newuser");
        assertThat(response.role()).isEqualTo("USER");
    }

    @Test
    void register_throwsUsernameAlreadyExists_whenUsernameTaken() {
        RegisterRequest request = new RegisterRequest("taken", "free@example.com", "plainPassword");
        when(userRepository.existsByUsername("taken")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(UsernameAlreadyExistsException.class);

        verify(userRepository, never()).save(any());
        verify(userRepository, never()).existsByEmail(any());
    }

    @Test
    void register_throwsEmailAlreadyExists_whenUsernameFreeButEmailTaken() {
        RegisterRequest request = new RegisterRequest("free", "taken@example.com", "plainPassword");
        when(userRepository.existsByUsername("free")).thenReturn(false);
        when(userRepository.existsByEmail("taken@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(EmailAlreadyExistsException.class);

        verify(userRepository, never()).save(any());
    }

    @Test
    void login_returnsToken_whenCredentialsAreValid() {
        User user = User.builder()
                .id(1L)
                .username("existing")
                .email("existing@example.com")
                .password("encodedPassword")
                .role(Role.USER)
                .build();

        LoginRequest request = new LoginRequest("existing", "plainPassword");
        when(userRepository.findByUsername("existing")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("plainPassword", "encodedPassword")).thenReturn(true);
        when(jwtService.generateToken(user)).thenReturn("generated-token");

        AuthResponse response = authService.login(request);

        assertThat(response.token()).isEqualTo("generated-token");
        assertThat(response.username()).isEqualTo("existing");
        assertThat(response.role()).isEqualTo("USER");
    }

    @Test
    void login_throwsInvalidCredentials_whenUserNotFound() {
        LoginRequest request = new LoginRequest("ghost", "whatever");
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void login_throwsInvalidCredentials_whenPasswordDoesNotMatch() {
        User user = User.builder()
                .id(1L)
                .username("existing")
                .email("existing@example.com")
                .password("encodedPassword")
                .role(Role.USER)
                .build();

        LoginRequest request = new LoginRequest("existing", "wrongPassword");
        when(userRepository.findByUsername("existing")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongPassword", "encodedPassword")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void login_failureMessageDoesNotRevealWhetherUsernameOrPasswordWasWrong() {
        LoginRequest unknownUser = new LoginRequest("ghost", "whatever");
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        String unknownUserMessage = catchInvalidCredentialsMessage(() -> authService.login(unknownUser));

        User user = User.builder()
                .id(1L)
                .username("existing")
                .email("existing@example.com")
                .password("encodedPassword")
                .role(Role.USER)
                .build();
        LoginRequest wrongPassword = new LoginRequest("existing", "wrongPassword");
        when(userRepository.findByUsername("existing")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongPassword", "encodedPassword")).thenReturn(false);

        String wrongPasswordMessage = catchInvalidCredentialsMessage(() -> authService.login(wrongPassword));

        assertThat(unknownUserMessage).isEqualTo(wrongPasswordMessage);
    }

    private String catchInvalidCredentialsMessage(Runnable action) {
        try {
            action.run();
        } catch (InvalidCredentialsException ex) {
            return ex.getMessage();
        }
        throw new AssertionError("Expected InvalidCredentialsException was not thrown");
    }
}
