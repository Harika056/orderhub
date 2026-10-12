package com.harika.orderhub.inventory;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Version;

/** Stock for one SKU. The @Version column makes concurrent updates fail instead of overselling. */
@Entity
public class StockItem {

    @Id
    private String sku;
    private int available;

    @Version
    private long version;

    protected StockItem() {
    }

    public StockItem(String sku, int available) {
        this.sku = sku;
        this.available = available;
    }

    public String getSku() {
        return sku;
    }

    public int getAvailable() {
        return available;
    }

    public void setAvailable(int available) {
        this.available = available;
    }
}
