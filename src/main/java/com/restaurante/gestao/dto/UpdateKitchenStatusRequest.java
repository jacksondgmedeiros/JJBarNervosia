package com.restaurante.gestao.dto;

import com.restaurante.gestao.model.KitchenStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateKitchenStatusRequest(
        @NotNull KitchenStatus status
) {
}
