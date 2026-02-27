package com.restaurante.gestao.repository;

import com.restaurante.gestao.model.KitchenStatus;
import com.restaurante.gestao.model.OrderTicket;
import com.restaurante.gestao.model.TableSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderTicketRepository extends JpaRepository<OrderTicket, Long> {
    List<OrderTicket> findByKitchenStatusInOrderByCreatedAtAsc(List<KitchenStatus> statuses);
    List<OrderTicket> findBySessionOrderByCreatedAtAsc(TableSession session);
}
