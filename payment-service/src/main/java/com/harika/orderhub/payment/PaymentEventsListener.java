package com.harika.orderhub.payment;

import com.harika.orderhub.common.Events;
import com.harika.orderhub.common.Json;
import com.harika.orderhub.common.Topics;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Simulated payment gateway. Orders above payment.max-amount are declined, and
 * payment.failure-rate (0.0 to 1.0) lets you inject random declines to exercise the failure path.
 */
@Component
public class PaymentEventsListener {

    private final PaymentRepository payments;
    private final KafkaTemplate<String, String> kafka;
    private final BigDecimal maxAmount;
    private final double failureRate;

    public PaymentEventsListener(PaymentRepository payments, KafkaTemplate<String, String> kafka,
                                 @Value("${payment.max-amount}") BigDecimal maxAmount,
                                 @Value("${payment.failure-rate}") double failureRate) {
        this.payments = payments;
        this.kafka = kafka;
        this.maxAmount = maxAmount;
        this.failureRate = failureRate;
    }

    @KafkaListener(topics = Topics.INVENTORY_RESERVED)
    public void onInventoryReserved(String payload) {
        Events.InventoryReserved event = Json.read(payload, Events.InventoryReserved.class);

        if (payments.existsById(event.orderId())) {
            return; // duplicate delivery
        }

        boolean declined = event.amount().compareTo(maxAmount) > 0
                || ThreadLocalRandom.current().nextDouble() < failureRate;

        if (declined) {
            payments.save(new Payment(event.orderId(), event.amount(), "FAILED"));
            kafka.send(Topics.PAYMENT_FAILED, event.orderId(), Json.write(
                    new Events.PaymentFailed(event.orderId(), "Payment declined")));
        } else {
            payments.save(new Payment(event.orderId(), event.amount(), "COMPLETED"));
            kafka.send(Topics.PAYMENT_COMPLETED, event.orderId(), Json.write(
                    new Events.PaymentCompleted(event.orderId())));
        }
    }
}
