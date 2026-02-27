package com.restaurante.gestao.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CreateOrderRequest(
        @NotNull @Min(1) Integer tableNumber,
        @NotNull Long productId,
        @NotNull @Min(1) Integer quantity,
        String notes
) {
}
