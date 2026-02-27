package com.restaurante.gestao.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "table_sessions")
public class TableSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Integer tableNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SessionStatus status = SessionStatus.OPEN;

    @Column(nullable = false)
    private LocalDateTime openedAt = LocalDateTime.now();

    public Long getId() { return id; }

    public Integer getTableNumber() { return tableNumber; }

    public void setTableNumber(Integer tableNumber) { this.tableNumber = tableNumber; }

    public SessionStatus getStatus() { return status; }

    public void setStatus(SessionStatus status) { this.status = status; }

    public LocalDateTime getOpenedAt() { return openedAt; }
}
