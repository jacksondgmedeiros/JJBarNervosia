package com.restaurante.gestao.dto;

import com.restaurante.gestao.model.SessionStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record WaiterSessionResponse(
        Long sessionId,
        Integer tableNumber,
        String customerName,
        Long waiterId,
        String waiterName,
        SessionStatus status,
        Boolean waiterFinalized,
        LocalDateTime openedAt,
        LocalDateTime closedAt,
        BigDecimal total,
        List<OrderTicketResponse> items
) {
}
