package com.flashshoes.model;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A time-boxed discount event tied to exactly one Product.
 *
 * limitedStock is an AtomicInteger so many purchase threads can attempt to decrement it
 * concurrently with a single atomic compare-and-set, without needing a full lock for the
 * common case. InventoryManager still wraps the *overall* reservation in a per-product lock
 * to keep FlashSale.limitedStock and Product.stockQuantity consistent with each other.
 */
public class FlashSale {

    private final String saleId;
    private final Product product;
    private double discountPercent;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private final AtomicInteger limitedStock;
    private volatile SaleStatus status;

    public FlashSale(String saleId, Product product, double discountPercent,
                      LocalDateTime startTime, LocalDateTime endTime, int limitedStock) {
        this.saleId = saleId;
        this.product = product;
        this.discountPercent = discountPercent;
        this.startTime = startTime;
        this.endTime = endTime;
        this.limitedStock = new AtomicInteger(limitedStock);
        this.status = SaleStatus.UPCOMING;
    }

    public boolean isActive() {
        LocalDateTime now = LocalDateTime.now();
        return status == SaleStatus.ACTIVE && now.isAfter(startTime) && now.isBefore(endTime);
    }

    /** Called by FlashSaleScheduler once per tick to move UPCOMING -> ACTIVE -> ENDED. */
    public void refreshStatus() {
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(startTime)) {
            status = SaleStatus.UPCOMING;
        } else if (now.isAfter(endTime) || limitedStock.get() <= 0 && status == SaleStatus.ACTIVE) {
            status = SaleStatus.ENDED;
        } else {
            status = SaleStatus.ACTIVE;
        }
    }

    public void activate() { status = SaleStatus.ACTIVE; }
    public void end() { status = SaleStatus.ENDED; }

    public double getDiscountedPrice() {
        return product.getPrice() * (1 - discountPercent / 100.0);
    }

    public String getSaleId() { return saleId; }
    public Product getProduct() { return product; }
    public double getDiscountPercent() { return discountPercent; }
    public LocalDateTime getStartTime() { return startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public AtomicInteger getLimitedStockRef() { return limitedStock; }
    public int getLimitedStock() { return limitedStock.get(); }
    public SaleStatus getStatus() { return status; }
    public void setStatus(SaleStatus status) { this.status = status; }

    @Override
    public String toString() {
        return "FlashSale{" + product.getName() + ", " + discountPercent + "% off, "
                + limitedStock.get() + " left, " + status + "}";
    }
}
