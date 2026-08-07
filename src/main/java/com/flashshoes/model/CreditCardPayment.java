package com.flashshoes.model;

public class CreditCardPayment implements PaymentMethod {

    private final String cardNumberMasked;

    public CreditCardPayment(String cardNumberLast4) {
        this.cardNumberMasked = "**** **** **** " + cardNumberLast4;
    }

    @Override
    public boolean processPayment(double amount) {
        // Simulated gateway check: any positive amount with a "card" succeeds.
        return amount > 0;
    }

    @Override
    public String getMethodName() { return "Credit Card (" + cardNumberMasked + ")"; }
}
