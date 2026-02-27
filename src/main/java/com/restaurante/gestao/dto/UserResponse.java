package com.restaurante.gestao.dto;

import com.restaurante.gestao.model.UserRole;

public record UserResponse(
        Long id,
        String fullName,
        String username,
        UserRole role,
        Boolean active
) {
}
