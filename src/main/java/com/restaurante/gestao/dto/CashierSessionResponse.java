package com.restaurante.gestao.dto;

import java.math.BigDecimal;
import java.util.List;

public record CashierSessionResponse(
        Long sessionId,
        Integer tableNumber,
        BigDecimal total,
        List<OrderTicketResponse> items
) {
}
