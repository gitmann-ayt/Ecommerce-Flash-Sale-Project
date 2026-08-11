package com.flashshoes.model;

public class CreditCardPayment implements PaymentMethod {

    private final String cardNumberMasked;

    public CreditCardPayment(String cardNumberLast4) {
        this.cardNumberMasked = "**** **** **** " + cardNumberLast4;
    }

    @Override
    public void processPayment(double amount) throws PaymentDeclinedException {
        // Simulated gateway check: any positive amount with a card succeeds.
        if (amount <= 0) {
            throw new PaymentDeclinedException("Card declined: invalid amount $" + amount);
        }
    }

    @Override
    public String getMethodName() { return "Credit Card (" + cardNumberMasked + ")"; }
}