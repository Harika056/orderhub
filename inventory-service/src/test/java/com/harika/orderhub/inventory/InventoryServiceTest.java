package com.harika.orderhub.inventory;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    StockRepository stock;
    @Mock
    ReservationRepository reservations;

    @InjectMocks
    InventoryService service;

    @Test
    void reservingDecrementsStockAndStoresReservation() {
        StockItem item = new StockItem("SKU-1", 10);
        when(reservations.existsById("order-1")).thenReturn(false);
        when(stock.findById("SKU-1")).thenReturn(Optional.of(item));

        assertTrue(service.reserve("order-1", "SKU-1", 3));

        assertEquals(7, item.getAvailable());
        verify(reservations).save(any(Reservation.class));
    }

    @Test
    void reservingFailsWhenThereIsNotEnoughStock() {
        StockItem item = new StockItem("SKU-1", 2);
        when(reservations.existsById("order-1")).thenReturn(false);
        when(stock.findById("SKU-1")).thenReturn(Optional.of(item));

        assertFalse(service.reserve("order-1", "SKU-1", 3));

        assertEquals(2, item.getAvailable());
        verify(reservations, never()).save(any(Reservation.class));
    }

    @Test
    void reservingTwiceForTheSameOrderOnlyTakesStockOnce() {
        when(reservations.existsById("order-1")).thenReturn(true);

        assertTrue(service.reserve("order-1", "SKU-1", 3));

        verify(stock, never()).findById(any());
    }

    @Test
    void releasingPutsStockBackAndDeletesTheReservation() {
        StockItem item = new StockItem("SKU-1", 7);
        Reservation reservation = new Reservation("order-1", "SKU-1", 3);
        when(reservations.findById("order-1")).thenReturn(Optional.of(reservation));
        when(stock.findById("SKU-1")).thenReturn(Optional.of(item));

        service.release("order-1");

        assertEquals(10, item.getAvailable());
        verify(reservations).delete(reservation);
    }
}
