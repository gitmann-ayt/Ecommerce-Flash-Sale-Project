package com.flashshoes.model;

public class CashOnDelivery implements PaymentMethod {

    @Override
    public void processPayment(double amount) {
        // Nothing to charge now; payment happens on delivery. Always succeeds at order time.
    }

    @Override
    public String getMethodName() { return "Cash on Delivery"; }
}