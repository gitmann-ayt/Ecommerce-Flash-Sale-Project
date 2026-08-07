package com.flashshoes;

import com.flashshoes.model.*;
import com.flashshoes.service.FileManager;
import com.flashshoes.service.InventoryManager;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Headless verification of the model/service layers (no JavaFX Application
 * needed, so it runs without a display). Checks: CSV loading parses correctly,
 * login hashing works, and — the important one — InventoryManager genuinely
 * prevents overselling under real concurrent load.
 */
public class SmokeTest {
    public static void main(String[] args) throws Exception {
        FileManager fm = new FileManager("data");

        List<Product> products = fm.loadProducts();
        System.out.println("Loaded products: " + products.size());
        if (products.isEmpty()) throw new AssertionError("No products loaded!");

        List<User> users = fm.loadUsers();
        System.out.println("Loaded users: " + users.size());
        User admin = users.stream().filter(u -> u.getRole().equals("ADMIN")).findFirst().orElseThrow();
        if (!admin.login("admin123")) throw new AssertionError("Admin login failed with seeded password!");
        if (admin.login("wrongpassword")) throw new AssertionError("Admin login succeeded with WRONG password!");
        System.out.println("Login hash check: OK");

        Map<String, Product> byId = new HashMap<>();
        for (Product p : products) byId.put(p.getProductId(), p);
        List<FlashSale> sales = fm.loadFlashSales(byId);
        System.out.println("Loaded flash sales: " + sales.size());
        long active = sales.stream().filter(FlashSale::isActive).count();
        System.out.println("Currently active flash sales: " + active);
        if (sales.isEmpty()) throw new AssertionError("No flash sales loaded!");

        // ---- concurrency correctness check ----
        FlashSale target = sales.get(0);
        target.activate();
        int stock = target.getLimitedStock();
        int attackers = stock + 15; // deliberately more threads than stock
        System.out.println("\nConcurrency test: " + attackers + " threads attacking a sale with stock=" + stock);

        InventoryManager im = new InventoryManager();
        ExecutorService pool = Executors.newFixedThreadPool(20);
        CountDownLatch latch = new CountDownLatch(attackers);
        AtomicInteger succeeded = new AtomicInteger(0);

        for (int i = 0; i < attackers; i++) {
            pool.submit(() -> {
                try {
                    if (im.reserveFlashSaleStock(target, 1)) succeeded.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }
        latch.await();
        pool.shutdown();

        System.out.println("Succeeded: " + succeeded.get() + " (expected " + stock + ")");
        System.out.println("Remaining stock: " + target.getLimitedStock() + " (expected 0)");
        if (succeeded.get() != stock) throw new AssertionError("Overselling or underselling detected! Expected " + stock + " got " + succeeded.get());
        if (target.getLimitedStock() != 0) throw new AssertionError("Stock did not reach exactly zero!");

        System.out.println("\nALL SMOKE TESTS PASSED.");
    }
}
