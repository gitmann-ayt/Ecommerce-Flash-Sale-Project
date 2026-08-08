package com.flashshoes.model;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Holds a customer's in-progress selection before checkout.
 * COMPOSITION: a Cart never outlives its owning Customer.
 */
public class Cart {

    private final Customer customer;
    private final Map<Product, Integer> items = new LinkedHashMap<>();
    private final Map<Product, String> selectedSizes = new LinkedHashMap<>();

    public Cart(Customer customer) {
        this.customer = customer;
    }

    public void addItem(Product product, int qty) {
        items.merge(product, qty, Integer::sum);
    }

    /** Adds an item remembering which size the customer picked on the product page. */
    public void addItem(Product product, int qty, String size) {
        items.merge(product, qty, Integer::sum);
        if (size != null && !size.isBlank()) selectedSizes.put(product, size);
    }

    public String getSelectedSize(Product product) {
        return selectedSizes.getOrDefault(product, "");
    }

    public void removeItem(Product product) {
        items.remove(product);
        selectedSizes.remove(product);
    }

    public void updateQuantity(Product product, int qty) {
        if (qty <= 0) items.remove(product);
        else items.put(product, qty);
    }

    public void clear() {
        items.clear();
        selectedSizes.clear();
    }

    public double getTotal() {
        double total = 0;
        for (Map.Entry<Product, Integer> e : items.entrySet()) {
            total += e.getKey().getPrice() * e.getValue();
        }
        return total;
    }

    public Map<Product, Integer> getItems() { return items; }
    public Customer getCustomer() { return customer; }
    public boolean isEmpty() { return items.isEmpty(); }
}
