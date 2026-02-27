package com.restaurante.gestao.controller;

import com.restaurante.gestao.dto.*;
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

    @GetMapping("/products")
    public ResponseEntity<List<ProductResponse>> listProducts() {
        return ResponseEntity.ok(restaurantService.listActiveProducts());
    }

    @PostMapping("/products")
    public ResponseEntity<ProductResponse> createProduct(@Valid @RequestBody ProductRequest request) {
        return ResponseEntity.ok(restaurantService.createProduct(request));
    }

    @GetMapping("/waiter/sessions/open")
    public ResponseEntity<List<WaiterSessionResponse>> waiterOpenSessions() {
        return ResponseEntity.ok(restaurantService.listOpenSessionsForWaiter());
    }

    @GetMapping("/waiter/sessions/history/today")
    public ResponseEntity<List<WaiterSessionResponse>> waiterHistoryToday() {
        return ResponseEntity.ok(restaurantService.listTodayHistoryForWaiter());
    }

    @PatchMapping("/waiter/sessions/{sessionId}/finalize")
    public ResponseEntity<Map<String, String>> finalizeForCashier(@PathVariable Long sessionId) {
        restaurantService.finalizeForCashier(sessionId);
        return ResponseEntity.ok(Map.of("message", "Comanda enviada para o caixa"));
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
