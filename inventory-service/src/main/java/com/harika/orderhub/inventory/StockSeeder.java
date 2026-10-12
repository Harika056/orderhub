package com.harika.orderhub.inventory;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/** Seeds a few SKUs on first start so the system can be tried straight away. */
@Component
public class StockSeeder implements CommandLineRunner {

    private final StockRepository stock;

    public StockSeeder(StockRepository stock) {
        this.stock = stock;
    }

    @Override
    public void run(String... args) {
        if (stock.count() > 0) {
            return;
        }
        for (int i = 1; i <= 5; i++) {
            stock.save(new StockItem("SKU-" + i, 100_000));
        }
    }
}
