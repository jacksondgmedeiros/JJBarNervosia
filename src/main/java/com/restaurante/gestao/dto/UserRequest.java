package com.restaurante.gestao.dto;

import com.restaurante.gestao.model.UserRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UserRequest(
        @NotBlank String fullName,
        @NotBlank String username,
        @NotBlank String password,
        @NotNull UserRole role
) {
}
