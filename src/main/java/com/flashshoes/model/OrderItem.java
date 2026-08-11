package com.flashshoes.model;

/**
 * One line of an Order. Storing priceAtPurchase separately from Product.price
 * is what makes a placed Order an immutable historical record even if the
 * catalogue price changes later. Size is captured at the same point, for the
 * same reason - what the customer actually ordered shouldn't change later.
 */
public class OrderItem {

    private final Product product;
    private final String size;
    private final int quantity;
    private final double priceAtPurchase;

    public OrderItem(Product product, String size, int quantity, double priceAtPurchase) {
        this.product = product;
        this.size = size;
        this.quantity = quantity;
        this.priceAtPurchase = priceAtPurchase;
    }

    public double getLineTotal() { return priceAtPurchase * quantity; }

    public Product getProduct() { return product; }
    public String getSize() { return size; }
    public int getQuantity() { return quantity; }
    public double getPriceAtPurchase() { return priceAtPurchase; }
}