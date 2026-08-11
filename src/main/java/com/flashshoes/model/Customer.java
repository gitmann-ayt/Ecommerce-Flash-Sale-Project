package com.flashshoes.model;

import java.util.ArrayList;
import java.util.List;

/**
 * A shopping account. INHERITANCE: extends User.
 * Owns exactly one Cart (composition) and accumulates Orders over time.
 */
public class Customer extends User {

    private final Cart cart;
    private final List<Order> orderHistory = new ArrayList<>();
    private double walletBalance;

    public Customer(String userId, String name, String email, String plainPassword, double walletBalance) {
        super(userId, name, email, plainPassword);
        this.cart = new Cart(this);
        this.walletBalance = walletBalance;
    }

    public Customer(String userId, String name, String email, String passwordHash, boolean isHash, double walletBalance) {
        super(userId, name, email, passwordHash, isHash);
        this.cart = new Cart(this);
        this.walletBalance = walletBalance;
    }

    @Override
    public String getRole() { return "CUSTOMER"; }

    public Cart getCart() { return cart; }

    public void addToCart(Product product, String size, int qty) {
        cart.addItem(product, size, qty);
    }

    public List<Order> getOrderHistory() { return orderHistory; }

    public void addOrderToHistory(Order order) { orderHistory.add(order); }

    public double getWalletBalance() { return walletBalance; }

    public void setWalletBalance(double walletBalance) { this.walletBalance = walletBalance; }

    public void deductWallet(double amount) { this.walletBalance -= amount; }
}
