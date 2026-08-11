package com.flashshoes.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Holds a customer's in-progress selection before checkout.
 * COMPOSITION: a Cart never outlives its owning Customer.
 * Lines are keyed by product+size, so the same shoe added in two different
 * sizes shows up as two separate cart lines, same as a real store's cart.
 */
public class Cart {

    private final Customer customer;
    private final Map<String, CartLine> lines = new LinkedHashMap<>();

    public Cart(Customer customer) {
        this.customer = customer;
    }

    private String key(Product product, String size) {
        return product.getProductId() + "|" + size;
    }

    public void addItem(Product product, String size, int qty) {
        String key = key(product, size);
        CartLine existing = lines.get(key);
        if (existing != null) {
            existing.setQuantity(existing.getQuantity() + qty);
        } else {
            lines.put(key, new CartLine(product, size, qty));
        }
    }

    public void removeLine(CartLine line) {
        lines.remove(key(line.getProduct(), line.getSize()));
    }

    public void updateQuantity(CartLine line, int qty) {
        if (qty <= 0) {
            removeLine(line);
        } else {
            line.setQuantity(qty);
        }
    }

    public void clear() {
        lines.clear();
    }

    public double getTotal() {
        double total = 0;
        for (CartLine line : lines.values()) {
            total += line.getLineTotal();
        }
        return total;
    }

    public List<CartLine> getLines() { return new ArrayList<>(lines.values()); }
    public Customer getCustomer() { return customer; }
    public boolean isEmpty() { return lines.isEmpty(); }
}