package com.flashshoes.model;

/**
 * One line in a Cart: a specific Product, in a specific size, at a quantity.
 * Splitting size out as part of the line (rather than just Product+qty) is
 * what lets the same shoe be added in two different sizes as two separate
 * lines, same as any real shoe store's cart.
 */
public class CartLine {

    private final Product product;
    private final String size;
    private int quantity;

    public CartLine(Product product, String size, int quantity) {
        this.product = product;
        this.size = size;
        this.quantity = quantity;
    }

    public double getLineTotal() {
        return product.getPrice() * quantity;
    }

    public Product getProduct() { return product; }
    public String getSize() { return size; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
}