package com.restaurante.gestao.dto;

import com.restaurante.gestao.model.ProductCategory;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record ProductRequest(
        @NotBlank String name,
        @NotNull ProductCategory category,
        @NotNull @DecimalMin("0.01") BigDecimal unitPrice
) {
}
