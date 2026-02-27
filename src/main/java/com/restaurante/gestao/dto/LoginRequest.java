package com.restaurante.gestao.dto;

import com.restaurante.gestao.model.UserRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record LoginRequest(
        @NotBlank String username,
        @NotBlank String password,
        @NotNull UserRole role
) {
}
