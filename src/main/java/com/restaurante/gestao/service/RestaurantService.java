package com.restaurante.gestao.service;

import com.restaurante.gestao.dto.*;
import com.restaurante.gestao.model.*;
import com.restaurante.gestao.repository.OrderTicketRepository;
import com.restaurante.gestao.repository.ProductRepository;
import com.restaurante.gestao.repository.TableSessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class RestaurantService {

    private final TableSessionRepository tableSessionRepository;
    private final OrderTicketRepository orderTicketRepository;
    private final ProductRepository productRepository;

    public RestaurantService(TableSessionRepository tableSessionRepository,
                             OrderTicketRepository orderTicketRepository,
                             ProductRepository productRepository) {
        this.tableSessionRepository = tableSessionRepository;
        this.orderTicketRepository = orderTicketRepository;
        this.productRepository = productRepository;
    }

    @Transactional
    public OrderTicketResponse createOrder(CreateOrderRequest request) {
        Product product = productRepository.findById(request.productId())
                .filter(Product::getActive)
                .orElseThrow(() -> new IllegalArgumentException("Produto de estoque não encontrado"));

        TableSession session = tableSessionRepository
                .findFirstByTableNumberAndStatusOrderByOpenedAtDesc(request.tableNumber(), SessionStatus.OPEN)
                .orElseGet(() -> {
                    TableSession newSession = new TableSession();
                    newSession.setTableNumber(request.tableNumber());
                    return tableSessionRepository.save(newSession);
                });

        if (Boolean.TRUE.equals(session.getWaiterFinalized())) {
            throw new IllegalArgumentException("Comanda já finalizada pelo garçom. Abra uma nova comanda após fechamento.");
        }

        OrderTicket ticket = new OrderTicket();
        ticket.setSession(session);
        ticket.setProduct(product);
        ticket.setItemName(product.getName());
        ticket.setQuantity(request.quantity());
        ticket.setUnitPrice(product.getUnitPrice());
        ticket.setNotes(request.notes());

        return toResponse(orderTicketRepository.save(ticket));
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> listActiveProducts() {
        return productRepository.findByActiveTrueOrderByCategoryAscNameAsc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public ProductResponse createProduct(ProductRequest request) {
        Product product = new Product();
        product.setName(request.name());
        product.setCategory(request.category());
        product.setUnitPrice(request.unitPrice());
        product.setActive(true);
        return toResponse(productRepository.save(product));
    }

    @Transactional(readOnly = true)
    public List<OrderTicketResponse> listKitchenTickets() {
        return orderTicketRepository
                .findByKitchenStatusInOrderByCreatedAtAsc(List.of(KitchenStatus.PENDING, KitchenStatus.PREPARING))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public OrderTicketResponse updateKitchenStatus(Long ticketId, KitchenStatus status) {
        OrderTicket ticket = orderTicketRepository.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("Pedido não encontrado"));
        ticket.setKitchenStatus(status);
        return toResponse(orderTicketRepository.save(ticket));
    }

    @Transactional(readOnly = true)
    public List<CashierSessionResponse> listOpenSessionsForCashier() {
        return tableSessionRepository.findByStatusOrderByOpenedAtAsc(SessionStatus.OPEN)
                .stream()
                .map(session -> {
                    List<OrderTicketResponse> items = listSessionItems(session);
                    BigDecimal total = sumItems(items);
                    return new CashierSessionResponse(session.getId(), session.getTableNumber(), session.getWaiterFinalized(), total, items);
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public List<WaiterSessionResponse> listOpenSessionsForWaiter() {
        return tableSessionRepository.findByStatusOrderByOpenedAtAsc(SessionStatus.OPEN)
                .stream()
                .map(this::toWaiterSessionResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<WaiterSessionResponse> listTodayHistoryForWaiter() {
        LocalDateTime start = LocalDate.now().atStartOfDay();
        LocalDateTime end = start.plusDays(1);

        return tableSessionRepository.findByOpenedAtBetweenOrderByOpenedAtDesc(start, end)
                .stream()
                .map(this::toWaiterSessionResponse)
                .toList();
    }

    @Transactional
    public void finalizeForCashier(Long sessionId) {
        TableSession session = tableSessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Comanda não encontrada"));
        if (session.getStatus() != SessionStatus.OPEN) {
            throw new IllegalArgumentException("Comanda já está fechada");
        }
        session.setWaiterFinalized(true);
        tableSessionRepository.save(session);
    }

    @Transactional
    public void closeSession(Long sessionId) {
        TableSession session = tableSessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Comanda não encontrada"));
        if (!Boolean.TRUE.equals(session.getWaiterFinalized())) {
            throw new IllegalArgumentException("A comanda precisa ser finalizada pelo garçom antes do caixa fechar");
        }
        session.setStatus(SessionStatus.CLOSED);
        session.setClosedAt(LocalDateTime.now());
        tableSessionRepository.save(session);
    }

    private WaiterSessionResponse toWaiterSessionResponse(TableSession session) {
        List<OrderTicketResponse> items = listSessionItems(session);
        BigDecimal total = sumItems(items);

        return new WaiterSessionResponse(
                session.getId(),
                session.getTableNumber(),
                session.getStatus(),
                session.getWaiterFinalized(),
                session.getOpenedAt(),
                session.getClosedAt(),
                total,
                items
        );
    }

    private List<OrderTicketResponse> listSessionItems(TableSession session) {
        return orderTicketRepository.findBySessionOrderByCreatedAtAsc(session)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private BigDecimal sumItems(List<OrderTicketResponse> items) {
        return items.stream()
                .map(OrderTicketResponse::total)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private OrderTicketResponse toResponse(OrderTicket ticket) {
        return new OrderTicketResponse(
                ticket.getId(),
                ticket.getSession().getTableNumber(),
                ticket.getItemName(),
                ticket.getQuantity(),
                ticket.getUnitPrice(),
                ticket.getTotal(),
                ticket.getNotes(),
                ticket.getKitchenStatus(),
                ticket.getCreatedAt()
        );
    }

    private ProductResponse toResponse(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getCategory(),
                product.getUnitPrice(),
                product.getActive()
        );
    }
}
