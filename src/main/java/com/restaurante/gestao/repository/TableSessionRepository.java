package com.restaurante.gestao.repository;

import com.restaurante.gestao.model.SessionStatus;
import com.restaurante.gestao.model.TableSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TableSessionRepository extends JpaRepository<TableSession, Long> {
    Optional<TableSession> findFirstByTableNumberAndStatusOrderByOpenedAtDesc(Integer tableNumber, SessionStatus status);
    List<TableSession> findByStatusOrderByOpenedAtAsc(SessionStatus status);
}
