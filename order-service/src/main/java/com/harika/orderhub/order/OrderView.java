package com.harika.orderhub.order;

import java.math.BigDecimal;
import java.time.Instant;

public record OrderView(String id, String sku, int quantity, BigDecimal amount,
                        OrderStatus status, Instant createdAt) {

    public static OrderView from(Order o) {
        return new OrderView(o.getId(), o.getSku(), o.getQuantity(), o.getAmount(),
                o.getStatus(), o.getCreatedAt());
    }
}
