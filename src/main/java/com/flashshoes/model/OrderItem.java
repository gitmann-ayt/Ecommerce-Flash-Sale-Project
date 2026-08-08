package com.flashshoes.model;

/**
 * One line of an Order. Storing priceAtPurchase separately from Product.price
 * is what makes a placed Order an immutable historical record even if the
 * catalogue price changes later.
 */
public class OrderItem {

    private final Product product;
    private final int quantity;
    private final double priceAtPurchase;
    private final String selectedSize;

    public OrderItem(Product product, int quantity, double priceAtPurchase) {
        this(product, quantity, priceAtPurchase, "");
    }

    public OrderItem(Product product, int quantity, double priceAtPurchase, String selectedSize) {
        this.product = product;
        this.quantity = quantity;
        this.priceAtPurchase = priceAtPurchase;
        this.selectedSize = selectedSize == null ? "" : selectedSize;
    }

    public double getLineTotal() { return priceAtPurchase * quantity; }

    public Product getProduct() { return product; }
    public int getQuantity() { return quantity; }
    public double getPriceAtPurchase() { return priceAtPurchase; }
    public String getSelectedSize() { return selectedSize; }
}
