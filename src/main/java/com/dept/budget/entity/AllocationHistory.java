package com.dept.budget.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/** A record of one change to an allocation: old amount, new amount and why. */
@Entity
@Table(name = "allocation_history")
public class AllocationHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer historyId;

    @ManyToOne
    @JoinColumn(name = "allocation_id")
    private Allocation allocation;

    private BigDecimal oldAmount;

    private BigDecimal newAmount;

    private String reason;

    private LocalDateTime changedAt = LocalDateTime.now();

    protected AllocationHistory() {
        // needed by JPA
    }

    public AllocationHistory(Allocation allocation, BigDecimal oldAmount, BigDecimal newAmount, String reason) {
        this.allocation = allocation;
        this.oldAmount = oldAmount;
        this.newAmount = newAmount;
        this.reason = reason;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("historyId", historyId);
        map.put("code", allocation.getCategory().getCode());
        map.put("name", allocation.getCategory().getName());
        map.put("oldAmount", oldAmount);
        map.put("newAmount", newAmount);
        map.put("reason", reason);
        map.put("changedAt", changedAt.toString());
        return map;
    }
}
