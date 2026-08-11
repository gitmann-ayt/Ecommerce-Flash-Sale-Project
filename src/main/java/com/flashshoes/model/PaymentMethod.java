package com.flashshoes.model;

/**
 * POLYMORPHISM: Order depends only on "some way to pay" - it never knows or
 * cares which concrete PaymentMethod was used. Adding a new payment method
 * later requires zero changes to Order or Cart (Open/Closed Principle).
 * A failed payment is a checked PaymentDeclinedException rather than a
 * boolean return, so every caller must explicitly handle the failure case.
 */
public interface PaymentMethod {
    void processPayment(double amount) throws PaymentDeclinedException;
    String getMethodName();
}