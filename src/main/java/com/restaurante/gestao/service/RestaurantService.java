package com.restaurante.gestao.service;

import com.restaurante.gestao.dto.CashierSessionResponse;
import com.restaurante.gestao.dto.CreateOrderRequest;
import com.restaurante.gestao.dto.OrderTicketResponse;
import com.restaurante.gestao.model.KitchenStatus;
import com.restaurante.gestao.model.OrderTicket;
import com.restaurante.gestao.model.SessionStatus;
import com.restaurante.gestao.model.TableSession;
import com.restaurante.gestao.repository.OrderTicketRepository;
import com.restaurante.gestao.repository.TableSessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class RestaurantService {

    private final TableSessionRepository tableSessionRepository;
    private final OrderTicketRepository orderTicketRepository;

    public RestaurantService(TableSessionRepository tableSessionRepository, OrderTicketRepository orderTicketRepository) {
        this.tableSessionRepository = tableSessionRepository;
        this.orderTicketRepository = orderTicketRepository;
    }

    @Transactional
    public OrderTicketResponse createOrder(CreateOrderRequest request) {
        TableSession session = tableSessionRepository
                .findFirstByTableNumberAndStatusOrderByOpenedAtDesc(request.tableNumber(), SessionStatus.OPEN)
                .orElseGet(() -> {
                    TableSession newSession = new TableSession();
                    newSession.setTableNumber(request.tableNumber());
                    return tableSessionRepository.save(newSession);
                });

        OrderTicket ticket = new OrderTicket();
        ticket.setSession(session);
        ticket.setItemName(request.itemName());
        ticket.setQuantity(request.quantity());
        ticket.setUnitPrice(request.unitPrice());
        ticket.setNotes(request.notes());

        return toResponse(orderTicketRepository.save(ticket));
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
                    List<OrderTicketResponse> items = orderTicketRepository
                            .findBySessionOrderByCreatedAtAsc(session)
                            .stream()
                            .map(this::toResponse)
                            .toList();

                    BigDecimal total = items.stream()
                            .map(OrderTicketResponse::total)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    return new CashierSessionResponse(session.getId(), session.getTableNumber(), total, items);
                })
                .toList();
    }

    @Transactional
    public void closeSession(Long sessionId) {
        TableSession session = tableSessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Comanda não encontrada"));
        session.setStatus(SessionStatus.CLOSED);
        tableSessionRepository.save(session);
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
}
