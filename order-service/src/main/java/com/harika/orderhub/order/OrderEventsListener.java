package com.harika.orderhub.order;

import com.harika.orderhub.common.Events;
import com.harika.orderhub.common.Json;
import com.harika.orderhub.common.Topics;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/** Moves an order to its final state based on what inventory and payment decided. */
@Component
public class OrderEventsListener {

    private final OrderRepository orders;

    public OrderEventsListener(OrderRepository orders) {
        this.orders = orders;
    }

    @KafkaListener(topics = Topics.PAYMENT_COMPLETED)
    public void onPaymentCompleted(String payload) {
        update(Json.read(payload, Events.PaymentCompleted.class).orderId(), OrderStatus.CONFIRMED);
    }

    @KafkaListener(topics = Topics.PAYMENT_FAILED)
    public void onPaymentFailed(String payload) {
        update(Json.read(payload, Events.PaymentFailed.class).orderId(), OrderStatus.CANCELLED);
    }

    @KafkaListener(topics = Topics.INVENTORY_REJECTED)
    public void onInventoryRejected(String payload) {
        update(Json.read(payload, Events.InventoryRejected.class).orderId(), OrderStatus.CANCELLED);
    }

    private void update(String orderId, OrderStatus status) {
        orders.findById(orderId).ifPresent(order -> {
            order.setStatus(status);
            orders.save(order);
        });
    }
}
