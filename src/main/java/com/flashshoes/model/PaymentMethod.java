package com.flashshoes.model;

/**
 * POLYMORPHISM: Order depends only on "some way to pay" — it never knows or
 * cares which concrete PaymentMethod was used. Adding a new payment method
 * later requires zero changes to Order or Cart (Open/Closed Principle).
 */
public interface PaymentMethod {
    boolean processPayment(double amount);
    String getMethodName();
}
