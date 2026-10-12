package com.harika.orderhub.order;

import com.harika.orderhub.common.Topics;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    OrderRepository orders;
    @Mock
    InventoryClient inventory;
    @Mock
    KafkaTemplate<String, String> kafka;
    @Mock
    StringRedisTemplate redis;

    @InjectMocks
    OrderService service;

    private final CreateOrderRequest request =
            new CreateOrderRequest("SKU-1", 2, new BigDecimal("49.90"));

    @Test
    void savesPendingOrderAndPublishesEventWhenStockLooksAvailable() {
        when(inventory.isAvailable("SKU-1", 2)).thenReturn(true);
        when(orders.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderService.PlaceResult result = service.place(request, null);

        assertEquals(OrderStatus.PENDING, result.order().getStatus());
        assertEquals(true, result.created());
        verify(kafka).send(eq(Topics.ORDER_CREATED), anyString(), anyString());
    }

    @Test
    void rejectsOrderAndPublishesNothingWhenStockIsInsufficient() {
        when(inventory.isAvailable("SKU-1", 2)).thenReturn(false);

        assertThrows(ResponseStatusException.class, () -> service.place(request, null));

        verify(orders, never()).save(any(Order.class));
        verify(kafka, never()).send(anyString(), anyString(), anyString());
    }
}
