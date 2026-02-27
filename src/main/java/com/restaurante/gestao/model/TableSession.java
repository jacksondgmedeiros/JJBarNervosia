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
    private Boolean waiterFinalized = false;

    @Column(nullable = false)
    private LocalDateTime openedAt = LocalDateTime.now();

    private LocalDateTime closedAt;

    public Long getId() { return id; }

    public Integer getTableNumber() { return tableNumber; }

    public void setTableNumber(Integer tableNumber) { this.tableNumber = tableNumber; }

    public SessionStatus getStatus() { return status; }

    public void setStatus(SessionStatus status) { this.status = status; }

    public Boolean getWaiterFinalized() { return waiterFinalized; }

    public void setWaiterFinalized(Boolean waiterFinalized) { this.waiterFinalized = waiterFinalized; }

    public LocalDateTime getOpenedAt() { return openedAt; }

    public LocalDateTime getClosedAt() { return closedAt; }

    public void setClosedAt(LocalDateTime closedAt) { this.closedAt = closedAt; }
}
