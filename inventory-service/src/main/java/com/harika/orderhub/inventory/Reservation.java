package com.harika.orderhub.inventory;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/** Remembers what was reserved for an order, so it can be released and duplicates are ignored. */
@Entity
public class Reservation {

    @Id
    private String orderId;
    private String sku;
    private int quantity;

    protected Reservation() {
    }

    public Reservation(String orderId, String sku, int quantity) {
        this.orderId = orderId;
        this.sku = sku;
        this.quantity = quantity;
    }

    public String getOrderId() {
        return orderId;
    }

    public String getSku() {
        return sku;
    }

    public int getQuantity() {
        return quantity;
    }
}
