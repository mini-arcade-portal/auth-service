package com.miniarcade.auth_service.auth.dto;

public record AuthResponse(
        String token,
        String username,
        String role
) {}