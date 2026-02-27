package com.restaurante.gestao.controller;

import com.restaurante.gestao.dto.CashierSessionResponse;
import com.restaurante.gestao.dto.CreateOrderRequest;
import com.restaurante.gestao.dto.OrderTicketResponse;
import com.restaurante.gestao.dto.UpdateKitchenStatusRequest;
import com.restaurante.gestao.service.RestaurantService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ApiController {

    private final RestaurantService restaurantService;

    public ApiController(RestaurantService restaurantService) {
        this.restaurantService = restaurantService;
    }

    @PostMapping("/orders")
    public ResponseEntity<OrderTicketResponse> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        return ResponseEntity.ok(restaurantService.createOrder(request));
    }

    @GetMapping("/kitchen/orders")
    public ResponseEntity<List<OrderTicketResponse>> kitchenOrders() {
        return ResponseEntity.ok(restaurantService.listKitchenTickets());
    }

    @PatchMapping("/kitchen/orders/{ticketId}")
    public ResponseEntity<OrderTicketResponse> updateKitchenStatus(
            @PathVariable Long ticketId,
            @Valid @RequestBody UpdateKitchenStatusRequest request
    ) {
        return ResponseEntity.ok(restaurantService.updateKitchenStatus(ticketId, request.status()));
    }

    @GetMapping("/cashier/sessions")
    public ResponseEntity<List<CashierSessionResponse>> cashierSessions() {
        return ResponseEntity.ok(restaurantService.listOpenSessionsForCashier());
    }

    @PatchMapping("/cashier/sessions/{sessionId}/close")
    public ResponseEntity<Map<String, String>> closeSession(@PathVariable Long sessionId) {
        restaurantService.closeSession(sessionId);
        return ResponseEntity.ok(Map.of("message", "Comanda fechada com sucesso"));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(Map.of("error", exception.getMessage()));
    }
}
