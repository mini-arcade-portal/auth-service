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
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new UsernameAlreadyExistsException(
                    "Username '" + request.username() + "' is already taken"
            );
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyExistsException(
                    "Email '" + request.email() + "' is already registered"
            );
        }

        User user = User.builder()
                .username(request.username())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .role(Role.USER)
                .build();

        User savedUser = userRepository.save(user);

        String token = jwtService.generateToken(savedUser);
        return new AuthResponse(token, savedUser.getUsername(), savedUser.getRole().name());
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid credentials"));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new InvalidCredentialsException("Invalid credentials");
        }

        String token = jwtService.generateToken(user);
        return new AuthResponse(token, user.getUsername(), user.getRole().name());
    }
}
