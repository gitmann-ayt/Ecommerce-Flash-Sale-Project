package com.flashshoes.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * A confirmed (or pending) purchase. Created as a snapshot at checkout time —
 * see OrderItem for why prices are copied rather than referenced.
 * COMPOSITION: OrderItems only exist as part of an Order.
 */
public class Order {

    private final String orderId;
    private final Customer customer;
    private final List<OrderItem> items;
    private double totalAmount;
    private final LocalDateTime timestamp;
    private volatile OrderStatus status;
    private String paymentMethodUsed;

    public Order(String orderId, Customer customer, List<OrderItem> items) {
        this.orderId = orderId;
        this.customer = customer;
        this.items = new ArrayList<>(items);
        this.timestamp = LocalDateTime.now();
        this.status = OrderStatus.PENDING;
        this.totalAmount = calculateTotal();
    }

    public double calculateTotal() {
        double total = 0;
        for (OrderItem item : items) total += item.getLineTotal();
        this.totalAmount = total;
        return total;
    }

    public void updateStatus(OrderStatus status) {
        this.status = status;
    }

    public String getOrderId() { return orderId; }
    public Customer getCustomer() { return customer; }
    public List<OrderItem> getItems() { return items; }
    public double getTotalAmount() { return totalAmount; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public OrderStatus getStatus() { return status; }
    public String getPaymentMethodUsed() { return paymentMethodUsed; }
    public void setPaymentMethodUsed(String paymentMethodUsed) { this.paymentMethodUsed = paymentMethodUsed; }

    @Override
    public String toString() {
        return "Order{" + orderId + ", " + customer.getName() + ", $" +
                String.format("%.2f", totalAmount) + ", " + status + "}";
    }
}
