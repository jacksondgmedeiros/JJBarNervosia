package com.restaurante.gestao.dto;

import com.restaurante.gestao.model.UserRole;

public record AuthResponse(
        Long userId,
        String fullName,
        String username,
        UserRole role
) {
}
