package com.restaurante.gestao.dto;

import com.restaurante.gestao.model.KitchenStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OrderTicketResponse(
        Long id,
        Integer tableNumber,
        String itemName,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal total,
        String notes,
        KitchenStatus kitchenStatus,
        LocalDateTime createdAt
) {
}
