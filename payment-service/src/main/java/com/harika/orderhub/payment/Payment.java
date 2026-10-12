package com.harika.orderhub.payment;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
public class Payment {

    @Id
    private String orderId;
    private BigDecimal amount;
    private String status;
    private Instant processedAt;

    protected Payment() {
    }

    public Payment(String orderId, BigDecimal amount, String status) {
        this.orderId = orderId;
        this.amount = amount;
        this.status = status;
        this.processedAt = Instant.now();
    }

    public String getOrderId() {
        return orderId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getStatus() {
        return status;
    }

    public Instant getProcessedAt() {
        return processedAt;
    }
}
