package com.harika.orderhub.order;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

/**
 * Synchronous stock pre-check against inventory-service (resolved through Eureka).
 *
 * If inventory-service is slow or down, retries with exponential backoff run first, the circuit
 * breaker then opens, and the fallback accepts the order anyway. The authoritative stock decision
 * is still made asynchronously by inventory-service when it consumes the order-created event.
 */
@Component
public class InventoryClient {

    private static final Logger log = LoggerFactory.getLogger(InventoryClient.class);

    private final RestClient restClient;

    public InventoryClient(@Qualifier("inventoryRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    @Retry(name = "inventory", fallbackMethod = "assumeAvailable")
    @CircuitBreaker(name = "inventory")
    public boolean isAvailable(String sku, int quantity) {
        try {
            StockView stock = restClient.get()
                    .uri("/inventory/{sku}", sku)
                    .retrieve()
                    .body(StockView.class);
            return stock != null && stock.available() >= quantity;
        } catch (HttpClientErrorException.NotFound e) {
            // Unknown SKU is a valid answer, not a service failure.
            return false;
        }
    }

    /** Used when inventory-service cannot be reached: keep taking orders. */
    public boolean assumeAvailable(String sku, int quantity, Throwable cause) {
        log.warn("Inventory pre-check unavailable for {} ({}); accepting order and letting the "
                + "async reservation decide", sku, cause.toString());
        return true;
    }

    record StockView(String sku, int available) {
    }
}
