package com.harika.orderhub.common;

import java.math.BigDecimal;

/** Event payloads that travel over Kafka as JSON. */
public final class Events {

    public record OrderCreated(String orderId, String sku, int quantity, BigDecimal amount) {
    }

    public record InventoryReserved(String orderId, String sku, int quantity, BigDecimal amount) {
    }

    public record InventoryRejected(String orderId, String reason) {
    }

    public record PaymentCompleted(String orderId) {
    }

    public record PaymentFailed(String orderId, String reason) {
    }

    private Events() {
    }
}
