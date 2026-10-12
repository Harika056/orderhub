package com.harika.orderhub.common;

/** Kafka topic names shared by every service. */
public final class Topics {
    public static final String ORDER_CREATED = "order-created";
    public static final String INVENTORY_RESERVED = "inventory-reserved";
    public static final String INVENTORY_REJECTED = "inventory-rejected";
    public static final String PAYMENT_COMPLETED = "payment-completed";
    public static final String PAYMENT_FAILED = "payment-failed";

    private Topics() {
    }
}
