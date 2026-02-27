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

    private static final String USER_HEADER = "X-User-Id";

    private final RestaurantService restaurantService;

    public ApiController(RestaurantService restaurantService) {
        this.restaurantService = restaurantService;
    }

    @PostMapping("/auth/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(restaurantService.login(request));
    }

    @PostMapping("/orders")
    public ResponseEntity<OrderTicketResponse> createOrder(@RequestHeader(USER_HEADER) Long userId,
                                                           @Valid @RequestBody CreateOrderRequest request) {
        return ResponseEntity.ok(restaurantService.createOrder(userId, request));
    }

    @GetMapping("/products")
    public ResponseEntity<List<ProductResponse>> listProducts(@RequestHeader(USER_HEADER) Long userId) {
        return ResponseEntity.ok(restaurantService.listActiveProducts(userId));
    }

    @PostMapping("/products")
    public ResponseEntity<ProductResponse> createProduct(@RequestHeader(USER_HEADER) Long userId,
                                                         @Valid @RequestBody ProductRequest request) {
        return ResponseEntity.ok(restaurantService.createProduct(userId, request));
    }

    @PutMapping("/products/{productId}")
    public ResponseEntity<ProductResponse> updateProduct(@RequestHeader(USER_HEADER) Long userId,
                                                         @PathVariable Long productId,
                                                         @Valid @RequestBody ProductRequest request) {
        return ResponseEntity.ok(restaurantService.updateProduct(userId, productId, request));
    }

    @DeleteMapping("/products/{productId}")
    public ResponseEntity<Map<String, String>> deleteProduct(@RequestHeader(USER_HEADER) Long userId,
                                                             @PathVariable Long productId) {
        restaurantService.deleteProduct(userId, productId);
        return ResponseEntity.ok(Map.of("message", "Produto removido do estoque"));
    }

    @GetMapping("/waiter/sessions/open")
    public ResponseEntity<List<WaiterSessionResponse>> waiterOpenSessions(@RequestHeader(USER_HEADER) Long userId) {
        return ResponseEntity.ok(restaurantService.listOpenSessionsForWaiter(userId));
    }

    @GetMapping("/waiter/sessions/history/today")
    public ResponseEntity<List<WaiterSessionResponse>> waiterHistoryToday(@RequestHeader(USER_HEADER) Long userId) {
        return ResponseEntity.ok(restaurantService.listTodayHistoryForWaiter(userId));
    }

    @PatchMapping("/waiter/sessions/{sessionId}/finalize")
    public ResponseEntity<Map<String, String>> finalizeForCashier(@RequestHeader(USER_HEADER) Long userId,
                                                                  @PathVariable Long sessionId) {
        restaurantService.finalizeForCashier(userId, sessionId);
        return ResponseEntity.ok(Map.of("message", "Comanda enviada para o caixa"));
    }

    @GetMapping("/kitchen/orders")
    public ResponseEntity<List<OrderTicketResponse>> kitchenOrders(@RequestHeader(USER_HEADER) Long userId) {
        return ResponseEntity.ok(restaurantService.listKitchenTickets(userId));
    }

    @PatchMapping("/kitchen/orders/{ticketId}")
    public ResponseEntity<OrderTicketResponse> updateKitchenStatus(
            @RequestHeader(USER_HEADER) Long userId,
            @PathVariable Long ticketId,
            @Valid @RequestBody UpdateKitchenStatusRequest request
    ) {
        return ResponseEntity.ok(restaurantService.updateKitchenStatus(userId, ticketId, request.status()));
    }

    @GetMapping("/cashier/sessions")
    public ResponseEntity<List<CashierSessionResponse>> cashierSessions(@RequestHeader(USER_HEADER) Long userId) {
        return ResponseEntity.ok(restaurantService.listOpenSessionsForCashier(userId));
    }

    @PatchMapping("/cashier/sessions/{sessionId}/close")
    public ResponseEntity<Map<String, String>> closeSession(@RequestHeader(USER_HEADER) Long userId,
                                                            @PathVariable Long sessionId) {
        restaurantService.closeSession(userId, sessionId);
        return ResponseEntity.ok(Map.of("message", "Comanda fechada com sucesso"));
    }

    @GetMapping("/admin/users")
    public ResponseEntity<List<UserResponse>> listUsers(@RequestHeader(USER_HEADER) Long userId) {
        return ResponseEntity.ok(restaurantService.listUsers(userId));
    }

    @PostMapping("/admin/users")
    public ResponseEntity<UserResponse> createUser(@RequestHeader(USER_HEADER) Long userId,
                                                   @Valid @RequestBody UserRequest request) {
        return ResponseEntity.ok(restaurantService.createUser(userId, request));
    }

    @PutMapping("/admin/users/{managedUserId}")
    public ResponseEntity<UserResponse> updateUser(@RequestHeader(USER_HEADER) Long userId,
                                                   @PathVariable Long managedUserId,
                                                   @Valid @RequestBody UserRequest request) {
        return ResponseEntity.ok(restaurantService.updateUser(userId, managedUserId, request));
    }

    @DeleteMapping("/admin/users/{managedUserId}")
    public ResponseEntity<Map<String, String>> deleteUser(@RequestHeader(USER_HEADER) Long userId,
                                                          @PathVariable Long managedUserId) {
        restaurantService.deleteUser(userId, managedUserId);
        return ResponseEntity.ok(Map.of("message", "Usuário removido"));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(Map.of("error", exception.getMessage()));
    }
}
