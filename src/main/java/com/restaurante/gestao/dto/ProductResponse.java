package com.restaurante.gestao.dto;

import com.restaurante.gestao.model.ProductCategory;

import java.math.BigDecimal;

public record ProductResponse(
        Long id,
        String name,
        ProductCategory category,
        BigDecimal unitPrice,
        Boolean active
) {
}
