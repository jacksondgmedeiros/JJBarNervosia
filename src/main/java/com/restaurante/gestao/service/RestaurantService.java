package com.restaurante.gestao.service;

import com.restaurante.gestao.dto.*;
import com.restaurante.gestao.model.*;
import com.restaurante.gestao.repository.AppUserRepository;
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
    private final AppUserRepository appUserRepository;

    public RestaurantService(TableSessionRepository tableSessionRepository,
                             OrderTicketRepository orderTicketRepository,
                             ProductRepository productRepository,
                             AppUserRepository appUserRepository) {
        this.tableSessionRepository = tableSessionRepository;
        this.orderTicketRepository = orderTicketRepository;
        this.productRepository = productRepository;
        this.appUserRepository = appUserRepository;
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        AppUser user = appUserRepository.findByUsernameIgnoreCaseAndActiveTrue(request.username())
                .orElseThrow(() -> new IllegalArgumentException("Usuário/senha inválidos"));

        if (!user.getPassword().equals(request.password()) || user.getRole() != request.role()) {
            throw new IllegalArgumentException("Usuário/senha inválidos para o setor selecionado");
        }

        return toAuthResponse(user);
    }

    @Transactional
    public OrderTicketResponse createOrder(Long userId, CreateOrderRequest request) {
        AppUser waiter = requireUserRole(userId, UserRole.WAITER, UserRole.ADMIN);
        Product product = productRepository.findById(request.productId())
                .filter(Product::getActive)
                .orElseThrow(() -> new IllegalArgumentException("Produto de estoque não encontrado"));

        TableSession session = tableSessionRepository
                .findFirstByTableNumberAndStatusOrderByOpenedAtDesc(request.tableNumber(), SessionStatus.OPEN)
                .orElseGet(() -> {
                    TableSession newSession = new TableSession();
                    newSession.setTableNumber(request.tableNumber());
                    newSession.setCustomerName(request.customerName().trim());
                    newSession.setWaiterId(waiter.getId());
                    newSession.setWaiterName(waiter.getFullName());
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
        ticket.setKitchenStatus(KitchenStatus.DRAFT);

        return toResponse(orderTicketRepository.save(ticket));
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> listActiveProducts(Long userId) {
        requireActiveUser(userId);
        return productRepository.findByActiveTrueOrderByCategoryAscNameAsc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public ProductResponse createProduct(Long userId, ProductRequest request) {
        requireUserRole(userId, UserRole.STOCK, UserRole.ADMIN);
        Product product = new Product();
        product.setName(request.name());
        product.setCategory(request.category());
        product.setUnitPrice(request.unitPrice());
        product.setActive(true);
        return toResponse(productRepository.save(product));
    }

    @Transactional
    public ProductResponse updateProduct(Long userId, Long productId, ProductRequest request) {
        requireUserRole(userId, UserRole.STOCK, UserRole.ADMIN);
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Produto não encontrado"));
        product.setName(request.name());
        product.setCategory(request.category());
        product.setUnitPrice(request.unitPrice());
        return toResponse(productRepository.save(product));
    }

    @Transactional
    public void deleteProduct(Long userId, Long productId) {
        requireUserRole(userId, UserRole.STOCK, UserRole.ADMIN);
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Produto não encontrado"));
        product.setActive(false);
        productRepository.save(product);
    }

    @Transactional(readOnly = true)
    public List<OrderTicketResponse> listKitchenTickets(Long userId) {
        requireUserRole(userId, UserRole.KITCHEN, UserRole.ADMIN);
        return orderTicketRepository
                .findByKitchenStatusInOrderByCreatedAtAsc(List.of(KitchenStatus.PENDING, KitchenStatus.PREPARING))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public OrderTicketResponse updateKitchenStatus(Long userId, Long ticketId, KitchenStatus status) {
        requireUserRole(userId, UserRole.KITCHEN, UserRole.ADMIN);
        OrderTicket ticket = orderTicketRepository.findById(ticketId)
                .orElseThrow(() -> new IllegalArgumentException("Pedido não encontrado"));
        ticket.setKitchenStatus(status);
        return toResponse(orderTicketRepository.save(ticket));
    }

    @Transactional(readOnly = true)
    public List<CashierSessionResponse> listOpenSessionsForCashier(Long userId) {
        requireUserRole(userId, UserRole.CASHIER, UserRole.ADMIN);
        return tableSessionRepository.findByStatusOrderByOpenedAtAsc(SessionStatus.OPEN)
                .stream()
                .map(session -> {
                    List<OrderTicketResponse> items = listSessionItems(session);
                    BigDecimal total = sumItems(items);
                    return new CashierSessionResponse(
                            session.getId(),
                            session.getTableNumber(),
                            session.getCustomerName(),
                            session.getWaiterId(),
                            session.getWaiterName(),
                            session.getWaiterFinalized(),
                            total,
                            items
                    );
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public List<WaiterSessionResponse> listOpenSessionsForWaiter(Long userId) {
        requireUserRole(userId, UserRole.WAITER, UserRole.ADMIN);
        return tableSessionRepository.findByStatusOrderByOpenedAtAsc(SessionStatus.OPEN)
                .stream()
                .map(this::toWaiterSessionResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<WaiterSessionResponse> listTodayHistoryForWaiter(Long userId) {
        requireUserRole(userId, UserRole.WAITER, UserRole.ADMIN);
        LocalDateTime start = LocalDate.now().atStartOfDay();
        LocalDateTime end = start.plusDays(1);

        return tableSessionRepository.findByOpenedAtBetweenOrderByOpenedAtDesc(start, end)
                .stream()
                .map(this::toWaiterSessionResponse)
                .toList();
    }

    @Transactional
    public void sendSessionToKitchen(Long userId, Long sessionId) {
        requireUserRole(userId, UserRole.WAITER, UserRole.ADMIN);
        TableSession session = tableSessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Comanda não encontrada"));

        List<OrderTicket> draftTickets = orderTicketRepository.findBySessionAndKitchenStatus(session, KitchenStatus.DRAFT);
        if (draftTickets.isEmpty()) {
            throw new IllegalArgumentException("Não há novos itens para enviar para cozinha");
        }

        draftTickets.forEach(ticket -> ticket.setKitchenStatus(KitchenStatus.PENDING));
        orderTicketRepository.saveAll(draftTickets);
    }

    @Transactional
    public void finalizeForCashier(Long userId, Long sessionId) {
        requireUserRole(userId, UserRole.WAITER, UserRole.ADMIN);
        TableSession session = tableSessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Comanda não encontrada"));
        if (session.getStatus() != SessionStatus.OPEN) {
            throw new IllegalArgumentException("Comanda já está fechada");
        }

        boolean hasDraftItems = !orderTicketRepository.findBySessionAndKitchenStatus(session, KitchenStatus.DRAFT).isEmpty();
        if (hasDraftItems) {
            throw new IllegalArgumentException("Existem itens ainda não enviados para cozinha");
        }

        session.setWaiterFinalized(true);
        tableSessionRepository.save(session);
    }

    @Transactional
    public void closeSession(Long userId, Long sessionId) {
        requireUserRole(userId, UserRole.CASHIER, UserRole.ADMIN);
        TableSession session = tableSessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Comanda não encontrada"));
        if (!Boolean.TRUE.equals(session.getWaiterFinalized())) {
            throw new IllegalArgumentException("A comanda precisa ser finalizada pelo garçom antes do caixa fechar");
        }
        session.setStatus(SessionStatus.CLOSED);
        session.setClosedAt(LocalDateTime.now());
        tableSessionRepository.save(session);
    }

    @Transactional(readOnly = true)
    public List<UserResponse> listUsers(Long userId) {
        requireUserRole(userId, UserRole.ADMIN);
        return appUserRepository.findByActiveTrueOrderByRoleAscFullNameAsc().stream()
                .map(this::toUserResponse)
                .toList();
    }

    @Transactional
    public UserResponse createUser(Long userId, UserRequest request) {
        requireUserRole(userId, UserRole.ADMIN);
        if (appUserRepository.existsByUsernameIgnoreCase(request.username())) {
            throw new IllegalArgumentException("Já existe usuário com esse login");
        }

        AppUser user = new AppUser();
        user.setFullName(request.fullName());
        user.setUsername(request.username());
        user.setPassword(request.password());
        user.setRole(request.role());
        user.setActive(true);

        return toUserResponse(appUserRepository.save(user));
    }

    @Transactional
    public UserResponse updateUser(Long requesterId, Long userId, UserRequest request) {
        requireUserRole(requesterId, UserRole.ADMIN);

        AppUser user = appUserRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado"));

        if (!user.getUsername().equalsIgnoreCase(request.username())
                && appUserRepository.existsByUsernameIgnoreCase(request.username())) {
            throw new IllegalArgumentException("Já existe usuário com esse login");
        }

        user.setFullName(request.fullName());
        user.setUsername(request.username());
        user.setPassword(request.password());
        user.setRole(request.role());
        user.setActive(true);
        return toUserResponse(appUserRepository.save(user));
    }

    @Transactional
    public void deleteUser(Long requesterId, Long userId) {
        requireUserRole(requesterId, UserRole.ADMIN);
        AppUser user = appUserRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado"));
        user.setActive(false);
        appUserRepository.save(user);
    }

    private WaiterSessionResponse toWaiterSessionResponse(TableSession session) {
        List<OrderTicketResponse> items = listSessionItems(session);
        BigDecimal total = sumItems(items);

        return new WaiterSessionResponse(
                session.getId(),
                session.getTableNumber(),
                session.getCustomerName(),
                session.getWaiterId(),
                session.getWaiterName(),
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

    private AuthResponse toAuthResponse(AppUser user) {
        return new AuthResponse(user.getId(), user.getFullName(), user.getUsername(), user.getRole());
    }

    private UserResponse toUserResponse(AppUser user) {
        return new UserResponse(user.getId(), user.getFullName(), user.getUsername(), user.getRole(), user.getActive());
    }

    private AppUser requireActiveUser(Long userId) {
        return appUserRepository.findById(userId)
                .filter(AppUser::getActive)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não autenticado"));
    }

    private AppUser requireUserRole(Long userId, UserRole... allowedRoles) {
        AppUser user = requireActiveUser(userId);
        for (UserRole allowedRole : allowedRoles) {
            if (user.getRole() == allowedRole) {
                return user;
            }
        }
        throw new IllegalArgumentException("Seu perfil não tem permissão para esta ação");
    }
}
