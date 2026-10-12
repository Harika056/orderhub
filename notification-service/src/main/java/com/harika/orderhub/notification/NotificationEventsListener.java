package com.harika.orderhub.notification;

import com.harika.orderhub.common.Events;
import com.harika.orderhub.common.Json;
import com.harika.orderhub.common.Topics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/** Turns order outcomes into customer notifications (stored and logged instead of emailed). */
@Component
public class NotificationEventsListener {

    private static final Logger log = LoggerFactory.getLogger(NotificationEventsListener.class);

    private final NotificationRepository notifications;

    public NotificationEventsListener(NotificationRepository notifications) {
        this.notifications = notifications;
    }

    @KafkaListener(topics = Topics.PAYMENT_COMPLETED)
    public void onPaymentCompleted(String payload) {
        String orderId = Json.read(payload, Events.PaymentCompleted.class).orderId();
        notify(orderId, "Your order is confirmed.");
    }

    @KafkaListener(topics = Topics.PAYMENT_FAILED)
    public void onPaymentFailed(String payload) {
        Events.PaymentFailed event = Json.read(payload, Events.PaymentFailed.class);
        notify(event.orderId(), "Your order was cancelled: " + event.reason() + ".");
    }

    @KafkaListener(topics = Topics.INVENTORY_REJECTED)
    public void onInventoryRejected(String payload) {
        Events.InventoryRejected event = Json.read(payload, Events.InventoryRejected.class);
        notify(event.orderId(), "Your order was cancelled: " + event.reason() + ".");
    }

    private void notify(String orderId, String message) {
        notifications.save(new Notification(orderId, message));
        log.info("Notification for order {}: {}", orderId, message);
    }
}
