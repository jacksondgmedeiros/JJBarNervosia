package com.restaurante.gestao.dto;

import java.util.List;

public record KitchenOrderResponse(
        Long sessionId,
        Integer tableNumber,
        String customerName,
        List<OrderTicketResponse> items
) {
}
