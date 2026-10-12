package com.harika.orderhub.inventory;

import com.harika.orderhub.common.Events;
import com.harika.orderhub.common.Json;
import com.harika.orderhub.common.Topics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class InventoryEventsListener {

    private static final Logger log = LoggerFactory.getLogger(InventoryEventsListener.class);
    private static final int MAX_ATTEMPTS = 5;

    private final InventoryService inventory;
    private final KafkaTemplate<String, String> kafka;

    public InventoryEventsListener(InventoryService inventory, KafkaTemplate<String, String> kafka) {
        this.inventory = inventory;
        this.kafka = kafka;
    }

    @KafkaListener(topics = Topics.ORDER_CREATED)
    public void onOrderCreated(String payload) {
        Events.OrderCreated event = Json.read(payload, Events.OrderCreated.class);

        if (reserveWithRetry(event)) {
            kafka.send(Topics.INVENTORY_RESERVED, event.orderId(), Json.write(
                    new Events.InventoryReserved(event.orderId(), event.sku(),
                            event.quantity(), event.amount())));
        } else {
            kafka.send(Topics.INVENTORY_REJECTED, event.orderId(), Json.write(
                    new Events.InventoryRejected(event.orderId(),
                            "Not enough stock for " + event.sku())));
        }
    }

    @KafkaListener(topics = Topics.PAYMENT_FAILED)
    public void onPaymentFailed(String payload) {
        inventory.release(Json.read(payload, Events.PaymentFailed.class).orderId());
    }

    /** Optimistic locking can reject a write when two orders hit the same SKU; try again. */
    private boolean reserveWithRetry(Events.OrderCreated event) {
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                return inventory.reserve(event.orderId(), event.sku(), event.quantity());
            } catch (OptimisticLockingFailureException e) {
                log.debug("Stock for {} changed under us (attempt {}/{})",
                        event.sku(), attempt, MAX_ATTEMPTS);
            }
        }
        log.warn("Gave up reserving {} for order {} after {} attempts",
                event.sku(), event.orderId(), MAX_ATTEMPTS);
        return false;
    }
}
