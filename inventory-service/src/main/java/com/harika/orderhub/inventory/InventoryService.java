package com.harika.orderhub.inventory;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class InventoryService {

    private final StockRepository stock;
    private final ReservationRepository reservations;

    public InventoryService(StockRepository stock, ReservationRepository reservations) {
        this.stock = stock;
        this.reservations = reservations;
    }

    /** Reserves stock for an order. Safe to call twice for the same order. */
    @Transactional
    public boolean reserve(String orderId, String sku, int quantity) {
        if (reservations.existsById(orderId)) {
            return true;
        }
        StockItem item = stock.findById(sku).orElse(null);
        if (item == null || item.getAvailable() < quantity) {
            return false;
        }
        item.setAvailable(item.getAvailable() - quantity);
        stock.save(item);
        reservations.save(new Reservation(orderId, sku, quantity));
        return true;
    }

    /** Puts reserved stock back, for example after a failed payment. */
    @Transactional
    public void release(String orderId) {
        reservations.findById(orderId).ifPresent(reservation -> {
            stock.findById(reservation.getSku()).ifPresent(item -> {
                item.setAvailable(item.getAvailable() + reservation.getQuantity());
                stock.save(item);
            });
            reservations.delete(reservation);
        });
    }

    @Transactional(readOnly = true)
    public Optional<StockItem> find(String sku) {
        return stock.findById(sku);
    }
}
