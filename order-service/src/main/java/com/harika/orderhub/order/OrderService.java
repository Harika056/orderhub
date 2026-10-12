package com.harika.orderhub.order;

import com.harika.orderhub.common.Events;
import com.harika.orderhub.common.Json;
import com.harika.orderhub.common.Topics;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

@Service
public class OrderService {

    public record PlaceResult(Order order, boolean created) {
    }

    private final OrderRepository orders;
    private final InventoryClient inventory;
    private final KafkaTemplate<String, String> kafka;
    private final StringRedisTemplate redis;

    public OrderService(OrderRepository orders, InventoryClient inventory,
                        KafkaTemplate<String, String> kafka, StringRedisTemplate redis) {
        this.orders = orders;
        this.inventory = inventory;
        this.kafka = kafka;
        this.redis = redis;
    }

    /**
     * Saves the order as PENDING and publishes order-created. The caller gets an answer straight
     * away; inventory and payment happen asynchronously.
     */
    public PlaceResult place(CreateOrderRequest request, String idempotencyKey) {
        String orderId = UUID.randomUUID().toString();
        String redisKey = null;

        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            redisKey = "idem:" + idempotencyKey;
            Boolean first = redis.opsForValue().setIfAbsent(redisKey, orderId, Duration.ofHours(24));
            if (!Boolean.TRUE.equals(first)) {
                String existingId = redis.opsForValue().get(redisKey);
                Optional<Order> existing = existingId == null
                        ? Optional.empty() : orders.findById(existingId);
                if (existing.isPresent()) {
                    return new PlaceResult(existing.get(), false);
                }
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "A request with this Idempotency-Key is still being processed");
            }
        }

        try {
            if (!inventory.isAvailable(request.sku(), request.quantity())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Insufficient stock for " + request.sku());
            }
            Order order = orders.save(new Order(orderId, request.sku(), request.quantity(),
                    request.amount()));
            kafka.send(Topics.ORDER_CREATED, orderId, Json.write(new Events.OrderCreated(
                    orderId, order.getSku(), order.getQuantity(), order.getAmount())));
            return new PlaceResult(order, true);
        } catch (RuntimeException e) {
            // Let the client retry with the same key if nothing was created.
            if (redisKey != null) {
                redis.delete(redisKey);
            }
            throw e;
        }
    }

    public Optional<Order> find(String id) {
        return orders.findById(id);
    }
}
